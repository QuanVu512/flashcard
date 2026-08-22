package com.flashcardapp.helper.path;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public final class SecurityPath {

    public static final String AUTH_LOGIN_API = "/api/auth/login";
    public static final String AUTH_REGISTER_API = "/api/auth/register";
    public static final String AUTH_LOGOUT_API = "/api/auth/logout";
    public static final String CSRF_API = "/api/auth/csrf";

    public static final String[] PUBLIC_AUTH_API_PATHS = {
            AUTH_LOGIN_API,
            AUTH_REGISTER_API,
            AUTH_LOGOUT_API,
            CSRF_API
    };

    private static final Set<String> COOKIE_AUTH_SKIP_PATHS = Set.of(PUBLIC_AUTH_API_PATHS);

    public static final String[] PUBLIC_PATHS = {
            "/",
            "/index.html",
            "/css/**",
            "/js/**",
            "/images/**",
            "/assets/**",
            "/views/**",
            "/favicon.ico",
            "/error",
            "/error/**",
            AppPath.LOGIN,
            AppPath.REGISTER,
            AppPath.ACCESS_DENIED
    };

    public static final String[] ADMIN_PATHS = {
            "/api/admin/**"
    };

    public static final String[] API_PATHS = {
            "/api/**"
    };

    public static boolean shouldSkipAccessTokenCookie(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestPath = request.getRequestURI();
        if (!contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return COOKIE_AUTH_SKIP_PATHS.contains(requestPath);
    }

    private SecurityPath() {
    }
}
