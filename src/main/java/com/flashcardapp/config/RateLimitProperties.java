package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        Boolean enabled,
        Integer apiCapacity,
        Long windowSeconds
) {

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }

    public int apiCapacityOrDefault() {
        return apiCapacity == null || apiCapacity < 1 ? 120 : apiCapacity;
    }

    public long windowSecondsOrDefault() {
        return windowSeconds == null || windowSeconds < 1 ? 60L : windowSeconds;
    }
}
