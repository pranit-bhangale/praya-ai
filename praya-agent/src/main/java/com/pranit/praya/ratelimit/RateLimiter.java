package com.pranit.praya.ratelimit;

import java.time.Duration;

/**
 * Generic, domain-agnostic RateLimiter contract.
 * Supports token-bucket rate limiting, burst management, and inter-call delay throttling.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire 1 permit for the given key without blocking.
     *
     * @param key the domain or operation key (e.g. "github", "linkedin", "client-ip")
     * @return true if permit was acquired, false otherwise
     */
    boolean tryAcquire(String key);

    /**
     * Attempts to acquire permits without blocking.
     */
    boolean tryAcquire(String key, int permits);

    /**
     * Acquires 1 permit, blocking/sleeping until available if necessary.
     */
    void acquire(String key);

    /**
     * Acquires the specified number of permits, blocking until available.
     */
    void acquire(String key, int permits);

    /**
     * Enforces the minimum configured delay interval between consecutive operations for the key.
     */
    void pause(String key);

    /**
     * Registers or updates the rate limit rule for a specific key.
     */
    void configureRule(String key, RateLimitRule rule);
}
