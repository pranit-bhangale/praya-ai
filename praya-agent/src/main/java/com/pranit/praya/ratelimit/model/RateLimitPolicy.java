package com.pranit.praya.ratelimit.model;

import com.pranit.praya.ratelimit.annotation.RateLimit;
import lombok.Builder;

/**
 * Immutable compiled rate limit policy parameters.
 */
@Builder
public record RateLimitPolicy(
        String name,
        int limit,
        long windowMs,
        long cooldownMs,
        RateLimit.KeyType key,
        boolean failOpen
) {

    public static RateLimitPolicy from(RateLimit a, String defaultName) {
        long windowMs = a.windowUnit().toMillis(a.window());
        long cooldownMs = a.cooldownUnit().toMillis(a.cooldown());
        if (a.limit() <= 0 || windowMs <= 0 || cooldownMs < 0) {
            throw new IllegalStateException("Invalid @RateLimit configuration on " + defaultName);
        }
        if (a.key() == RateLimit.KeyType.EXPRESSION && a.keyExpression().isBlank()) {
            throw new IllegalStateException("keyExpression is required when key=EXPRESSION on " + defaultName);
        }
        String name = a.name().isBlank() ? defaultName : a.name();
        return RateLimitPolicy.builder()
                .name(name)
                .limit(a.limit())
                .windowMs(windowMs)
                .cooldownMs(cooldownMs)
                .key(a.key())
                .failOpen(a.failOpen())
                .build();
    }
}
