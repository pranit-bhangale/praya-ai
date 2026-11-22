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

    int limit() default 5;

    long window() default 60;

    TimeUnit windowUnit() default TimeUnit.SECONDS;

    long cooldown() default 30;

    TimeUnit cooldownUnit() default TimeUnit.SECONDS;

    KeyType key() default KeyType.IP;

    String keyExpression() default "";

    String name() default "";

    boolean failOpen() default true;

    enum KeyType {
        IP,
        EXPRESSION
    }
}
