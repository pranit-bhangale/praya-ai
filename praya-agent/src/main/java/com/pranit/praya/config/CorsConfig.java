package com.pranit.praya.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Centralized Web MVC and CORS Configuration dynamically driven by {@link CorsProperties}.
 * Configures Spring 7 API Versioning and CORS mappings from application.yaml.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping(this.corsProperties.pathPattern())
                .allowedOriginPatterns(this.corsProperties.allowedOrigins().toArray(new String[0]))
                .allowedMethods(this.corsProperties.allowedMethods().toArray(new String[0]))
                .allowedHeaders(this.corsProperties.allowedHeaders())
                .exposedHeaders(this.corsProperties.exposedHeaders().toArray(new String[0]))
                .allowCredentials(this.corsProperties.allowCredentials())
                .maxAge(this.corsProperties.maxAge());
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = this.corsProperties.allowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            for (String origin : origins) {
                config.addAllowedOriginPattern(origin);
            }
        } else {
            config.addAllowedOriginPattern("http://localhost:*");
            config.addAllowedOriginPattern("http://127.0.0.1:*");
            config.addAllowedOriginPattern("https://*.vercel.app");
        }
        config.setAllowedMethods(this.corsProperties.allowedMethods());
        config.addAllowedHeader(this.corsProperties.allowedHeaders());
        config.setExposedHeaders(this.corsProperties.exposedHeaders());
        config.setAllowCredentials(this.corsProperties.allowCredentials());
        config.setMaxAge(this.corsProperties.maxAge());
        source.registerCorsConfiguration(this.corsProperties.pathPattern(), config);
        return new CorsFilter(source);
    }

    @Override
    public void configureApiVersioning(@NonNull ApiVersionConfigurer configurer) {
        configurer.useRequestHeader("X-API-Version")
                .setDefaultVersion("v1")
                .setVersionRequired(false);
    }
}
