package com.pranit.praya.ratelimit.aop;

import com.pranit.praya.ratelimit.annotation.RateLimit;
import com.pranit.praya.ratelimit.annotation.RateLimits;
import com.pranit.praya.ratelimit.exception.RateLimitExceededException;
import com.pranit.praya.ratelimit.model.RateLimitPolicy;
import com.pranit.praya.ratelimit.model.RateLimitResult;
import com.pranit.praya.ratelimit.service.ClientIdentity;
import com.pranit.praya.ratelimit.service.RateLimitMetrics;
import com.pranit.praya.ratelimit.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect enforcing declarative rate limits via {@link RateLimit} and {@link RateLimits}.
 * Supports both HTTP controller endpoints and method calls.
 * Enforces composite key security for public expressions (IP + Hashed identifier) to prevent cross-tenant DoS.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@ConditionalOnProperty(name = "praya.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private final RateLimitService service;
    private final ClientIdentity identity;
    private final RateLimitMetrics metrics;
    private final ParameterNameDiscoverer paramNames = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<Method, List<CompiledPolicy>> policyCache = new ConcurrentHashMap<>();

    public RateLimitAspect(RateLimitService service, ClientIdentity identity, RateLimitMetrics metrics) {
        this.service = service;
        this.identity = identity;
        this.metrics = metrics;
    }

    @Around("@annotation(com.pranit.praya.ratelimit.annotation.RateLimit) "
            + "|| @annotation(com.pranit.praya.ratelimit.annotation.RateLimits)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        final Method declared = ((MethodSignature) pjp.getSignature()).getMethod();
        final Method method = AopUtils.getMostSpecificMethod(declared, pjp.getTarget().getClass());
        final List<CompiledPolicy> policies = this.policyCache.computeIfAbsent(method, this::compile);

        HttpServletRequest request = null;
        HttpServletResponse response = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            request = attrs.getRequest();
            response = attrs.getResponse();
        }

        long minRemaining = Long.MAX_VALUE;
        int minLimit = 0;

        for (CompiledPolicy c : policies) {
            Key key = resolveKey(c, request, pjp, method);
            RateLimitResult r = this.service.check(c.policy(), key.scope(), key.id());

            if (!r.allowed()) {
                long retrySeconds = Math.max(1, Math.ceilDiv(r.retryAfterMs(), 1000L));
                throw new RateLimitExceededException(retrySeconds);
            }

            if (r.remaining() >= 0 && r.remaining() < minRemaining) {
                minRemaining = r.remaining();
                minLimit = c.policy().limit();
            }
        }

        if (response != null && minRemaining != Long.MAX_VALUE) {
            response.setIntHeader("X-RateLimit-Limit", minLimit);
            response.setHeader("X-RateLimit-Remaining", Long.toString(minRemaining));
        }

        return pjp.proceed();
    }

    private List<CompiledPolicy> compile(Method method) {
        Set<RateLimit> annotations = AnnotatedElementUtils
                .getMergedRepeatableAnnotations(method, RateLimit.class, RateLimits.class);

        List<CompiledPolicy> out = new ArrayList<>(annotations.size());
        int index = 0;
        for (RateLimit a : annotations) {
            String defaultName = method.getDeclaringClass().getSimpleName()
                    + "#" + method.getName() + "#" + (index++);
            RateLimitPolicy policy = RateLimitPolicy.from(a, defaultName);
            Expression expr = a.key() == RateLimit.KeyType.EXPRESSION
                    ? this.parser.parseExpression(a.keyExpression())
                    : null;
            out.add(new CompiledPolicy(policy, expr));
        }
        return List.copyOf(out);
    }

    private Key resolveKey(CompiledPolicy c, HttpServletRequest req, ProceedingJoinPoint pjp, Method method) {
        String ip = this.identity.ip(req);

        return switch (c.policy().key()) {
            case IP -> new Key("ip", ip);
            case EXPRESSION -> {
                String value = evaluateExpression(c, pjp, method);
                if (value == null || value.isBlank()) {
                    yield new Key("ip", ip);
                }
                // Security Boundary: Combine IP + Hashed Expression Value.
                // On public endpoints without login, this guarantees an attacker cannot lock out
                // or exhaust the rate limit quota of other users by guessing their conversationId.
                yield new Key("x", ip + ":" + ClientIdentity.hash(value));
            }
        };
    }

    private String evaluateExpression(CompiledPolicy c, ProceedingJoinPoint pjp, Method method) {
        try {
            MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                    pjp.getTarget(), method, pjp.getArgs(), this.paramNames
            );
            Object value = c.keyExpr().getValue(ctx);
            if (value == null || value.toString().isBlank()) {
                this.metrics.keyFallback(c.policy().name(), "blank");
                return null;
            }
            return value.toString();
        } catch (Exception ex) {
            this.metrics.keyFallback(c.policy().name(), "error");
            log.warn("[RATE-LIMIT] [EXPRESSION-ERROR] Method {} keyExpression evaluation failed: {}", method.getName(), ex.getMessage());
            return null;
        }
    }

    private record CompiledPolicy(RateLimitPolicy policy, Expression keyExpr) {
    }

    private record Key(String scope, String id) {
    }
}
