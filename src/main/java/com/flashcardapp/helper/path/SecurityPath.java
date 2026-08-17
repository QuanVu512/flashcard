package com.flashcardapp.helper.path;

public final class SecurityPath {

    public static final String[] PUBLIC_PATHS = {
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/error",
            "/error/**",
            AppPath.LOGIN,
            AppPath.REGISTER,
            AppPath.ACCESS_DENIED
    };

    public static final String[] ADMIN_PATHS = {
            "/admin/**"
    };

    public static final String[] API_PATHS = {
            "/api/**"
    };

    private SecurityPath() {
    }
}
