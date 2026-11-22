package com.pranit.praya.ratelimit;

import com.pranit.praya.constant.ApiConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free, generic Token-Bucket RateLimiter supporting multi-key throttling,
 * burst allowances, and inter-call delay enforcement.
 * Fully compatible with Java 25 Virtual Threads (wait-free CAS updates and non-pinning sleep).
 */
@Slf4j
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private final Map<String, RateLimitRule> configuredRules = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<BucketState>> buckets = new ConcurrentHashMap<>();
    private final RateLimitRule defaultRule = RateLimitRule.defaultRule();

    public TokenBucketRateLimiter(
            @Value("${praya.rate-limiter.api-delay-ms:${app.github.api-delay-ms:50}}") long delayMs
    ) {
        long effectiveDelay = Math.max(0, delayMs);
        // Pre-configure standard domain rules
        configureRule(ApiConstants.RATE_LIMIT_KEY_GITHUB, RateLimitRule.builder()
                .permitsPerSecond(5.0)
                .burstCapacity(10.0)
                .minDelayMs(effectiveDelay)
                .build());

        configureRule(ApiConstants.RATE_LIMIT_KEY_LINKEDIN, RateLimitRule.builder()
                .permitsPerSecond(1.0)
                .burstCapacity(2.0)
                .minDelayMs(Math.max(500, effectiveDelay * 10))
                .build());

        configureRule(ApiConstants.RATE_LIMIT_KEY_PORTFOLIO, RateLimitRule.builder()
                .permitsPerSecond(10.0)
                .burstCapacity(20.0)
                .minDelayMs(Math.min(50, effectiveDelay))
                .build());
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        if (permits <= 0) return true;
        AtomicReference<BucketState> ref = getOrCreateBucket(key);

        while (true) {
            BucketState current = ref.get();
            long nowNanos = System.nanoTime();
            double elapsedSeconds = Math.max(0, nowNanos - current.lastRefillNanos()) / 1_000_000_000.0;
            double refilledTokens = Math.min(
                    current.rule().burstCapacity(),
                    current.tokens() + (elapsedSeconds * current.rule().permitsPerSecond())
            );

            if (refilledTokens < permits) {
                return false;
            }

            BucketState next = new BucketState(
                    refilledTokens - permits,
                    nowNanos,
                    System.currentTimeMillis(),
                    current.rule()
            );

            if (ref.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    @Override
    public void acquire(String key) {
        acquire(key, 1);
    }

    @Override
    public void acquire(String key, int permits) {
        pause(key);
        while (!tryAcquire(key, permits)) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for rate limiter permit on key: " + key, e);
            }
        }
    }

    @Override
    public void pause(String key) {
        AtomicReference<BucketState> ref = buckets.get(key);
        if (ref == null) return;

        RateLimitRule rule = ref.get().rule();
        long minDelay = rule.minDelayMs();
        if (minDelay <= 0) return;

        long elapsed = System.currentTimeMillis() - ref.get().lastAcquiredMillis();
        long remaining = minDelay - elapsed;
        if (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while enforcing rate limit pause on key: " + key, e);
            }
        }
    }

    @Override
    public void configureRule(String key, RateLimitRule rule) {
        log.info("Configuring generic rate limiter rule for [{}]: {} permits/sec, burst={}, minDelay={}ms",
                key, rule.permitsPerSecond(), rule.burstCapacity(), rule.minDelayMs());
        configuredRules.put(key, rule);
        buckets.compute(key, (k, existing) -> {
            long now = System.nanoTime();
            return new AtomicReference<>(new BucketState(rule.burstCapacity(), now, 0L, rule));
        });
    }

    private AtomicReference<BucketState> getOrCreateBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            RateLimitRule rule = configuredRules.getOrDefault(k, defaultRule);
            long now = System.nanoTime();
            return new AtomicReference<>(new BucketState(rule.burstCapacity(), now, 0L, rule));
        });
    }

    private record BucketState(
            double tokens,
            long lastRefillNanos,
            long lastAcquiredMillis,
            RateLimitRule rule
    ) {}
}
