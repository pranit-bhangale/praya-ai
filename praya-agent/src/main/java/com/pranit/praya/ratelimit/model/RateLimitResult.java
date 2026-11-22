package com.pranit.praya.ratelimit.model;

import lombok.Builder;

/**
 * Result of evaluating a rate limit request.
 *
 * @param allowed      True if the request is allowed, false if rejected
 * @param retryAfterMs Milliseconds until the client is permitted to retry
 * @param remaining    Remaining requests permitted within the current window (-1 if unknown/fail-open)
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
