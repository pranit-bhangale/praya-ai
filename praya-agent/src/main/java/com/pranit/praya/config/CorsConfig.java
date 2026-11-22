package com.pranit.praya.config;

import com.pranit.praya.constant.CorsConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Centralized Web MVC and CORS Configuration for the Praya backend application.
 * Configures Spring 7 API Versioning and CORS mappings.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping(CorsConstants.API_PATH_PATTERN)
                .allowedOrigins(CorsConstants.ALLOWED_ORIGINS)
                .allowedMethods(CorsConstants.ALLOWED_METHODS)
                .allowedHeaders(CorsConstants.ALLOWED_HEADERS)
                .exposedHeaders(CorsConstants.EXPOSED_HEADERS)
                .allowCredentials(true)
                .maxAge(CorsConstants.MAX_AGE_SECONDS);
    }

    @Override
    public void configureApiVersioning(@NonNull ApiVersionConfigurer configurer) {
        configurer.useRequestHeader("X-API-Version")
                .setDefaultVersion("v1")
                .setVersionRequired(false);
    }
}
