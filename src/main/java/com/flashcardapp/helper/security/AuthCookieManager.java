package com.flashcardapp.helper.security;

import com.flashcardapp.config.AuthCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Component
public class AuthCookieManager {

    private static final String COOKIE_PATH = "/";

    private final AuthCookieProperties properties;

    public AuthCookieManager(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public void writeAccessToken(HttpServletResponse response, String token, long expiresInSeconds) {
        writeCookie(response, properties.resolvedName(), token, Duration.ofSeconds(Math.max(0, expiresInSeconds)));
    }

    public void writeRefreshToken(HttpServletResponse response, String token, Duration duration) {
        writeCookie(response, properties.resolvedRefreshName(), token, duration);
    }

    public void writeTrustedDevice(HttpServletResponse response, String token, Duration duration) {
        writeCookie(response, properties.resolvedTrustedDeviceName(), token, duration);
    }

    public void writeGoogleLink(HttpServletResponse response, String token, Duration duration) {
        writeCookie(response, properties.resolvedGoogleLinkName(), token, duration);
    }

    public void writeOauthReturn(HttpServletResponse response, String path, Duration duration) {
        String encodedPath = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(path.getBytes(StandardCharsets.UTF_8));
        writeCookie(response, properties.resolvedOauthReturnName(), encodedPath, duration);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, properties.resolvedRefreshName());
    }

    public Optional<String> readTrustedDevice(HttpServletRequest request) {
        return readCookie(request, properties.resolvedTrustedDeviceName());
    }

    public Optional<String> readGoogleLink(HttpServletRequest request) {
        return readCookie(request, properties.resolvedGoogleLinkName());
    }

    public Optional<String> readOauthReturn(HttpServletRequest request) {
        return readCookie(request, properties.resolvedOauthReturnName())
                .flatMap(this::decodeOauthReturnPath);
    }

    private void writeCookie(HttpServletResponse response, String name, String token, Duration duration) {
        ResponseCookie cookie = cookieBuilder(name, token)
                .maxAge(duration.isNegative() ? Duration.ZERO : duration)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessToken(HttpServletResponse response) {
        clearCookie(response, properties.resolvedName());
    }

    public void clearRefreshToken(HttpServletResponse response) {
        clearCookie(response, properties.resolvedRefreshName());
    }

    public void clearTrustedDevice(HttpServletResponse response) {
        clearCookie(response, properties.resolvedTrustedDeviceName());
    }

    public void clearGoogleLink(HttpServletResponse response) {
        clearCookie(response, properties.resolvedGoogleLinkName());
    }

    public void clearOauthReturn(HttpServletResponse response) {
        clearCookie(response, properties.resolvedOauthReturnName());
    }

    public String cookieName() {
        return properties.resolvedName();
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = cookieBuilder(name, "")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private Optional<String> decodeOauthReturnPath(String encodedPath) {
        try {
            return Optional.of(new String(
                    Base64.getUrlDecoder().decode(encodedPath),
                    StandardCharsets.UTF_8
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.resolvedSameSite())
                .path(COOKIE_PATH);
    }
}
