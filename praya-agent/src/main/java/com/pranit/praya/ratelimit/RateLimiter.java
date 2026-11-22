package com.pranit.praya.ratelimit;

/**
 * Unified RateLimiter interface supporting programmatic permit acquisition,
 * throttling, and cooperative delays backed by the centralized in-memory sliding-window engine.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire 1 permit for the given key without blocking.
     *
     * @param key the domain or operation key (e.g. "github", "linkedin", "portfolio")
     * @return true if permit was acquired, false otherwise
     */
    boolean tryAcquire(String key);

    /**
     * Acquires 1 permit, cooperatively pausing the calling thread (non-pinning) until available.
     */
    void acquire(String key);

    /**
     * Acquires the specified number of permits, pausing until available.
     */
    void acquire(String key, int permits);

    /**
     * Enforces the minimum configured pause between consecutive operations for the key.
     */
    void pause(String key);
}
