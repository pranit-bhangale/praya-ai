package com.pranit.praya.ratelimit.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Micrometer metrics collector for in-memory rate limiting operations.
 * Caches meters to avoid repetitive registry lookups on the request hot-path.
 */
@Component
public class RateLimitMetrics {

    public static final String OUTCOME_ALLOWED = "allowed";
    public static final String OUTCOME_BLOCKED = "blocked";
    public static final String OUTCOME_ERROR = "error";

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void request(String policyName, String outcome) {
        this.counters.computeIfAbsent("req|" + policyName + "|" + outcome, _ ->
                Counter.builder("ratelimit.requests")
                        .description("Rate limiter decisions")
                        .tag("name", policyName)
                        .tag("outcome", outcome)
                        .register(this.registry)).increment();
    }

    public void keyFallback(String policyName, String reason) {
        this.counters.computeIfAbsent("fb|" + policyName + "|" + reason, _ ->
                Counter.builder("ratelimit.key.fallback")
                        .description("Rate limit key expression fallback")
                        .tag("name", policyName)
                        .tag("reason", reason)
                        .register(this.registry)).increment();
    }

    public Timer checkTimer(String policyName) {
        return this.timers.computeIfAbsent(policyName, n ->
                Timer.builder("ratelimit.check.latency")
                        .description("Latency of in-memory sliding window check")
                        .tag("name", n)
                        .publishPercentileHistogram()
                        .register(this.registry));
    }

    public Timer.Sample startTimer() {
        return Timer.start(this.registry);
    }
}
