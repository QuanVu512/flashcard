package com.flashcardapp.config;

import com.flashcardapp.helper.path.SecurityPath;
import com.flashcardapp.helper.security.AuthCookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver authorizationHeaderResolver = new DefaultBearerTokenResolver();
    private final AuthCookieManager authCookieManager;

    public CookieBearerTokenResolver(AuthCookieManager authCookieManager) {
        this.authCookieManager = authCookieManager;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String headerToken = authorizationHeaderResolver.resolve(request);
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        if (SecurityPath.shouldSkipAccessTokenCookie(request)) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> authCookieManager.cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
