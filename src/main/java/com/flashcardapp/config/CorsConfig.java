package com.flashcardapp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final List<String> DEFAULT_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> DEFAULT_HEADERS = List.of("Content-Type", "Authorization", "X-XSRF-TOKEN", "X-Requested-With", "X-Request-Id", "Accept", "Origin");
    private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of("X-Request-Id");
    private static final long DEFAULT_MAX_AGE_SECONDS = 1800L;

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(clean(properties.allowedOrigins()));
        configuration.setAllowedOriginPatterns(clean(properties.allowedOriginPatterns()));
        configuration.setAllowedMethods(defaultIfEmpty(properties.allowedMethods(), DEFAULT_METHODS));
        configuration.setAllowedHeaders(defaultIfEmpty(properties.allowedHeaders(), DEFAULT_HEADERS));
        configuration.setExposedHeaders(defaultIfEmpty(properties.exposedHeaders(), DEFAULT_EXPOSED_HEADERS));
        configuration.setAllowCredentials(properties.allowCredentials() == null || properties.allowCredentials());
        configuration.setMaxAge(properties.maxAge() == null ? DEFAULT_MAX_AGE_SECONDS : properties.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> defaultIfEmpty(List<String> values, List<String> defaultValues) {
        List<String> cleanedValues = clean(values);
        return CollectionUtils.isEmpty(cleanedValues) ? defaultValues : cleanedValues;
    }

    private List<String> clean(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
