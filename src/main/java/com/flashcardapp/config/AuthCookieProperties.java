package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "app.security.cookie")
public record AuthCookieProperties(
        String name,
        Boolean secure,
        String sameSite
) {

    private static final String DEFAULT_NAME = "flashcard_access_token";
    private static final String DEFAULT_SAME_SITE = "Lax";
    private static final Set<String> VALID_SAME_SITE_VALUES = Set.of("lax", "strict", "none");

    public String resolvedName() {
        return StringUtils.hasText(name) ? name.trim() : DEFAULT_NAME;
    }

    public boolean isSecure() {
        return Boolean.TRUE.equals(secure);
    }

    public String resolvedSameSite() {
        String value = StringUtils.hasText(sameSite) ? sameSite.trim() : DEFAULT_SAME_SITE;
        if (!VALID_SAME_SITE_VALUES.contains(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE chỉ chấp nhận Lax, Strict hoặc None");
        }
        if ("none".equalsIgnoreCase(value) && !isSecure()) {
            throw new IllegalStateException("AUTH_COOKIE_SECURE phải là true khi AUTH_COOKIE_SAME_SITE=None");
        }
        return value;
    }
}
