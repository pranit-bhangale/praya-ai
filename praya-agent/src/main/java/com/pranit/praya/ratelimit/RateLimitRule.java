package com.pranit.praya.ratelimit;

import lombok.Builder;

/**
 * Configuration rule defining token-bucket and delay constraints for a rate-limiting key.
 *
 * @param permitsPerSecond continuous token refill rate per second
 * @param burstCapacity maximum burst capacity / bucket size
 * @param minDelayMs minimum pause interval in milliseconds between consecutive calls
 */
@Builder
public record RateLimitRule(
        double permitsPerSecond,
        double burstCapacity,
        long minDelayMs
) {
    public static RateLimitRule defaultRule() {
        return RateLimitRule.builder()
                .permitsPerSecond(10.0)
                .burstCapacity(20.0)
                .minDelayMs(50)
                .build();
    }
}
