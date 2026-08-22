package com.flashcardapp.helper.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BrowserCsrfRequestMatcher implements RequestMatcher {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
            return false;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return !StringUtils.startsWithIgnoreCase(authorization, BEARER_PREFIX);
    }
}
