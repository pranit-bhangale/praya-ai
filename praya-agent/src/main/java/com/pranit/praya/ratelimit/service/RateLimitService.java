package com.pranit.praya.ratelimit.service;

import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.ratelimit.RateLimiter;
import com.pranit.praya.ratelimit.annotation.RateLimit;
import com.pranit.praya.ratelimit.model.RateLimitPolicy;
import com.pranit.praya.ratelimit.model.RateLimitResult;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Centralized in-memory sliding-window rate-limiting engine.
 * Serves both declarative API rate limiting (via {@link RateLimit})
 * and programmatic tool client throttling (via {@link RateLimiter}).
 */
@Slf4j
@Service
public class RateLimitService implements RateLimiter {

    public static final long DEFAULT_IDLE_EVICTION_THRESHOLD_MS = 120_000L;

    private final Map<String, InMemorySlidingWindowBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, RateLimitPolicy> domainPolicies = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAcquiredTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Long> domainMinDelays = new ConcurrentHashMap<>();
    private final RateLimitMetrics metrics;

    public RateLimitService(
            RateLimitMetrics metrics,
            @Value("${praya.rate-limiter.api-delay-ms:${app.github.api-delay-ms:50}}") long defaultDelayMs
    ) {
        this.metrics = metrics;

        long effectiveDelay = Math.max(0, defaultDelayMs);
        // Pre-configure standard domain policies using sliding window
        configureDomain(ApiConstants.RATE_LIMIT_KEY_GITHUB, 5, 1000, 0, effectiveDelay);
        configureDomain(ApiConstants.RATE_LIMIT_KEY_LINKEDIN, 1, 1000, 0, Math.max(500, effectiveDelay * 10));
        configureDomain(ApiConstants.RATE_LIMIT_KEY_PORTFOLIO, 10, 1000, 0, Math.min(50, effectiveDelay));
    }

    public void configureDomain(String domainKey, int limit, long windowMs, long cooldownMs, long minDelayMs) {
        RateLimitPolicy policy = RateLimitPolicy.builder()
                .name(domainKey)
                .limit(limit)
                .windowMs(windowMs)
                .cooldownMs(cooldownMs)
                .key(RateLimit.KeyType.EXPRESSION)
                .failOpen(true)
                .build();
        this.domainPolicies.put(domainKey, policy);
        this.domainMinDelays.put(domainKey, Math.max(0, minDelayMs));
    }

    @Override
    public boolean tryAcquire(String key) {
        RateLimitPolicy policy = getOrCreatePolicy(key);
        return check(policy, "tool", key).allowed();
    }

    @Override
    public void acquire(String key) {
        acquire(key, 1);
    }

    @Override
    public void acquire(String key, int permits) {
        pause(key);
        RateLimitPolicy policy = getOrCreatePolicy(key);
        final String bucketKey = "rl:{" + policy.name() + ":tool:" + key + "}";

        try {
            InMemorySlidingWindowBucket bucket = this.buckets.computeIfAbsent(
                    bucketKey, _ -> new InMemorySlidingWindowBucket()
            );

            bucket.acquire(policy, permits);
            this.lastAcquiredTimestamps.put(key, System.currentTimeMillis());
            this.metrics.request(policy.name(), RateLimitMetrics.OUTCOME_ALLOWED);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate limit permit on key: " + key, e);
        } catch (Exception ex) {
            this.metrics.request(policy.name(), RateLimitMetrics.OUTCOME_ERROR);
            log.error("[RATE-LIMIT] [ACQUIRE-ERROR] Unexpected error acquiring permit for policy '{}': {}", policy.name(), ex.getMessage(), ex);
            if (!policy.failOpen()) {
                throw ex;
            }
        }
    }

    @Override
    public void pause(String key) {
        Long minDelay = this.domainMinDelays.get(key);
        if (minDelay == null || minDelay <= 0) return;
        Long last = this.lastAcquiredTimestamps.get(key);
        if (last == null) return;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = minDelay - elapsed;
        if (remaining > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while enforcing rate limit pause on key: " + key, e);
            }
        }
    }

    private RateLimitPolicy getOrCreatePolicy(String key) {
        return this.domainPolicies.computeIfAbsent(key, k -> RateLimitPolicy.builder()
                .name(k)
                .limit(10)
                .windowMs(1000)
                .cooldownMs(0)
                .key(RateLimit.KeyType.EXPRESSION)
                .failOpen(true)
                .build());
    }

    /**
     * Evaluates permit availability for the given policy, scope, and identifier.
     */
    public RateLimitResult check(RateLimitPolicy policy, String scope, String id) {
        final String bucketKey = "rl:{" + policy.name() + ":" + scope + ":" + id + "}";
        final Timer.Sample sample = this.metrics.startTimer();

        try {
            InMemorySlidingWindowBucket bucket = this.buckets.computeIfAbsent(
                    bucketKey, _ -> new InMemorySlidingWindowBucket()
            );

            RateLimitResult result = bucket.check(policy);

            this.metrics.request(policy.name(), result.allowed()
                    ? RateLimitMetrics.OUTCOME_ALLOWED
                    : RateLimitMetrics.OUTCOME_BLOCKED);

            return result;

        } catch (Exception ex) {
            this.metrics.request(policy.name(), RateLimitMetrics.OUTCOME_ERROR);
            log.error("[RATE-LIMIT] [CHECK-ERROR] Error evaluating rate limit for policy '{}': {}", policy.name(), ex.getMessage(), ex);

            if (policy.failOpen()) {
                log.warn("[RATE-LIMIT] [FAIL-OPEN] Policy '{}' failed open to preserve service availability", policy.name());
                return RateLimitResult.permit(-1);
            }

            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rate limiter service temporarily unavailable");
        } finally {
            sample.stop(this.metrics.checkTimer(policy.name()));
        }
    }

    /**
     * Periodic background cleanup removing idle buckets to prevent memory leaks.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanIdleBuckets() {
        final long now = System.currentTimeMillis();
        final int beforeSize = this.buckets.size();

        this.buckets.entrySet().removeIf(entry -> entry.getValue().isIdle(now, DEFAULT_IDLE_EVICTION_THRESHOLD_MS));

        final int evicted = beforeSize - this.buckets.size();
        if (evicted > 0) {
            log.debug("[RATE-LIMIT] [BUCKET-CLEANUP] Evicted {} idle rate limit buckets. Remaining active: {}", evicted, this.buckets.size());
        }
    }
}
