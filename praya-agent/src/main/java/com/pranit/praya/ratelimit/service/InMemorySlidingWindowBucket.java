package com.pranit.praya.ratelimit.service;

import com.pranit.praya.ratelimit.model.RateLimitPolicy;
import com.pranit.praya.ratelimit.model.RateLimitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe in-memory sliding window bucket replicating the Redis Lua rate-limiting algorithm.
 * Uses fine-grained per-bucket locks to eliminate contention across distinct clients.
 * Fully compatible with Virtual Threads (non-pinning locks and condition await).
 */
public class InMemorySlidingWindowBucket {

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition permitAvailable = this.lock.newCondition();
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private volatile long cooldownUntilMs = 0;
    private volatile long lastAccessTimeMs = System.currentTimeMillis();

    /**
     * Checks permit availability according to the specified policy.
     */
    public RateLimitResult check(RateLimitPolicy policy) {
        this.lock.lock();
        try {
            long now = System.currentTimeMillis();
            this.lastAccessTimeMs = now;

            // 1. Active cooldown check
            if (this.cooldownUntilMs > now) {
                long retryAfterMs = this.cooldownUntilMs - now;
                return RateLimitResult.reject(retryAfterMs);
            }

            // 2. Slide window: remove expired requests
            long windowStart = now - policy.windowMs();
            while (!this.timestamps.isEmpty() && this.timestamps.peekFirst() <= windowStart) {
                this.timestamps.pollFirst();
            }

            // 3. Limit check
            int count = this.timestamps.size();
            if (count >= policy.limit()) {
                if (policy.cooldownMs() > 0) {
                    this.cooldownUntilMs = now + policy.cooldownMs();
                    this.timestamps.clear(); // Fresh start after cooldown lockout
                    return RateLimitResult.reject(policy.cooldownMs());
                }
                // No cooldown: retry when the oldest request exits the sliding window
                long oldest = this.timestamps.peekFirst() != null ? this.timestamps.peekFirst() : windowStart;
                long retryAfterMs = Math.max(1, (oldest + policy.windowMs()) - now);
                return RateLimitResult.reject(retryAfterMs);
            }

            // 4. Accept request
            this.timestamps.addLast(now);
            long remaining = policy.limit() - count - 1;
            return RateLimitResult.permit(remaining);

        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Acquires permits, cooperatively awaiting on a condition variable until permits become available.
     * Eliminates busy-waiting and Thread.sleep loops by calculating exact required wait times
     * and parking via Condition.await (virtual-thread friendly, non-pinning).
     */
    public void acquire(RateLimitPolicy policy, int permits) throws InterruptedException {
        if (permits <= 0) {
            return;
        }
        int effectivePermits = Math.min(permits, policy.limit());
        this.lock.lockInterruptibly();
        try {
            while (true) {
                long now = System.currentTimeMillis();
                this.lastAccessTimeMs = now;

                // 1. Active cooldown check
                if (this.cooldownUntilMs > now) {
                    long waitMs = this.cooldownUntilMs - now;
                    this.permitAvailable.await(waitMs, TimeUnit.MILLISECONDS);
                    continue;
                }

                // 2. Slide window: remove expired requests
                long windowStart = now - policy.windowMs();
                while (!this.timestamps.isEmpty() && this.timestamps.peekFirst() <= windowStart) {
                    this.timestamps.pollFirst();
                }

                // 3. Limit check: do we have capacity for effectivePermits?
                int currentCount = this.timestamps.size();
                if (currentCount + effectivePermits <= policy.limit()) {
                    for (int i = 0; i < effectivePermits; i++) {
                        this.timestamps.addLast(now);
                    }
                    return;
                }

                // 4. Over capacity: calculate exact wait time until enough slots open up
                int neededFreed = currentCount + effectivePermits - policy.limit();
                long targetTimestamp = getTimestampAtIndex(neededFreed - 1, windowStart);
                long waitMs = Math.max(1L, (targetTimestamp + policy.windowMs()) - now);

                this.permitAvailable.await(waitMs, TimeUnit.MILLISECONDS);
            }
        } finally {
            this.lock.unlock();
        }
    }

    private long getTimestampAtIndex(int index, long defaultValue) {
        if (this.timestamps.isEmpty() || index < 0) {
            return defaultValue;
        }
        int i = 0;
        for (Long ts : this.timestamps) {
            if (i == index) {
                return ts != null ? ts : defaultValue;
            }
            i++;
        }
        return defaultValue;
    }

    /**
     * Determines whether this bucket is idle and safe for background eviction.
     */
    public boolean isIdle(long now, long idleThresholdMs) {
        if (now - this.lastAccessTimeMs < idleThresholdMs) {
            return false;
        }
        if (this.cooldownUntilMs > now) {
            return false;
        }
        this.lock.lock();
        try {
            return this.timestamps.isEmpty() || (this.timestamps.peekLast() != null && (now - this.timestamps.peekLast() > idleThresholdMs));
        } finally {
            this.lock.unlock();
        }
    }
}
