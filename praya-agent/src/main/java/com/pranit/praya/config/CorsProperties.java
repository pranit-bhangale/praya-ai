package com.pranit.praya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for CORS settings dynamically loaded from application.yaml.
 */
@ConfigurationProperties(prefix = "praya.cors")
public record CorsProperties(
        String pathPattern,
        List<String> allowedOrigins,
        List<String> allowedMethods,
        String allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        long maxAge
) {
    public CorsProperties {
        if (pathPattern == null || pathPattern.isBlank()) {
            pathPattern = "/api/**";
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("https://pranitbhangale.vercel.app", "http://localhost:3000", "http://localhost:5173");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isBlank()) {
            allowedHeaders = "*";
        }
        if (exposedHeaders == null || exposedHeaders.isEmpty()) {
            exposedHeaders = List.of("Content-Type", "Cache-Control", "Connection", "X-API-Version");
        }
        if (maxAge <= 0) {
            maxAge = 3600L;
        }
    }
}
