package com.pranit.praya.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Configuration properties for CORS settings dynamically loaded from application.yaml.
 */
@ConfigurationProperties(prefix = "praya.cors")
public record CorsProperties(
        @DefaultValue("/api/**") String pathPattern,
        @DefaultValue("https://pranitbhangale.vercel.app") List<String> allowedOrigins,
        @DefaultValue({"GET", "POST", "OPTIONS"}) List<String> allowedMethods,
        @DefaultValue({"Content-Type", "Authorization", "Accept", "X-Requested-With"}) List<String> allowedHeaders,
        @DefaultValue({"Content-Type", "Cache-Control", "Connection", "X-API-Version"}) List<String> exposedHeaders,
        @DefaultValue("true") boolean allowCredentials,
        @DefaultValue("3600") long maxAge
) {

    public CorsProperties {
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalStateException("praya.cors: allowed-origins cannot contain '*' when allow-credentials is true");
        }
    }
}