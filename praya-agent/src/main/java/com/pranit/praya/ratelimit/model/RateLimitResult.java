package com.pranit.praya.ratelimit.model;

import lombok.Builder;

/**
 * Result of evaluating a rate limit request.
 */
@Builder
public record RateLimitResult(
        boolean allowed,
        long retryAfterMs,
        long remaining
) {

    public static RateLimitResult permit(long remaining) {
        return RateLimitResult.builder()
                .allowed(true)
                .retryAfterMs(0)
                .remaining(remaining)
                .build();
    }

    public static RateLimitResult reject(long retryAfterMs) {
        return RateLimitResult.builder()
                .allowed(false)
                .retryAfterMs(retryAfterMs)
                .remaining(0)
                .build();
    }
}
