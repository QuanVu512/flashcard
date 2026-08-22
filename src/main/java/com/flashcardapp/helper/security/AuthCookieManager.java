package com.flashcardapp.helper.security;

import com.flashcardapp.config.AuthCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieManager {

    private static final String COOKIE_PATH = "/";

    private final AuthCookieProperties properties;

    public AuthCookieManager(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public void writeAccessToken(HttpServletResponse response, String token, long expiresInSeconds) {
        ResponseCookie cookie = cookieBuilder(token)
                .maxAge(Duration.ofSeconds(Math.max(0, expiresInSeconds)))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessToken(HttpServletResponse response) {
        ResponseCookie cookie = cookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String cookieName() {
        return properties.resolvedName();
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(cookieName(), value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.resolvedSameSite())
                .path(COOKIE_PATH);
    }
}
