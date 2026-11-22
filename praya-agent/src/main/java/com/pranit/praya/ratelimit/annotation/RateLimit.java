package com.pranit.praya.ratelimit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for declarative rate limiting on API endpoints or methods.
 * Implements an in-memory sliding-window log algorithm with cooldown lockout.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /**
     * Max requests allowed within the sliding window.
     */
    int limit() default 5;

    /**
     * Sliding window duration.
     */
    long window() default 60;

    /**
     * Unit for the window duration.
     */
    TimeUnit windowUnit() default TimeUnit.SECONDS;

    /**
     * Lockout duration after the limit is exceeded.
     * 0 = no cooldown (pure sliding window, retries as soon as oldest entry leaves the window).
     */
    long cooldown() default 30;

    /**
     * Unit for the cooldown duration.
     */
    TimeUnit cooldownUnit() default TimeUnit.SECONDS;

    /**
     * Client identity resolution strategy: IP or EXPRESSION.
     */
    KeyType key() default KeyType.IP;

    /**
     * Spring SpEL expression when key = KeyType.EXPRESSION (e.g. "#request.conversationId").
     */
    String keyExpression() default "";

    /**
     * Unique bucket and metric name (defaults to ClassName#methodName#index).
     */
    String name() default "";

    /**
     * When true, permits requests in the rare event of an internal limiter evaluation error.
     */
    boolean failOpen() default true;

    enum KeyType {
        IP,
        EXPRESSION
    }
}
