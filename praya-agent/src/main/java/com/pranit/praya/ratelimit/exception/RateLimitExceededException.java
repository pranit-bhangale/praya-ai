package com.pranit.praya.ratelimit.exception;

/**
 * Exception thrown when a request violates configured rate limiting policies.
 * Suppresses stack trace generation for maximum throughput during high-load traffic.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.", null, false, false);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return this.retryAfterSeconds;
    }
}
