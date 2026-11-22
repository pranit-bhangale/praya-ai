package com.pranit.praya.ratelimit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration enabling declarative annotation-driven rate limiting.
 * Can be disabled in tests or environments via praya.ratelimit.enabled=false.
 */
@Configuration
@ConditionalOnProperty(name = "praya.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.pranit.praya.ratelimit")
public class RateLimitConfig {
}
