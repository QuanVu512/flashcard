package com.flashcardapp.helper.security;

import com.flashcardapp.config.GoogleAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuthFailureHandler implements AuthenticationFailureHandler {

    private final AuthCookieManager cookieManager;
    private final GoogleAuthProperties googleProperties;

    public GoogleOAuthFailureHandler(AuthCookieManager cookieManager,
                                     GoogleAuthProperties googleProperties) {
        this.cookieManager = cookieManager;
        this.googleProperties = googleProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        cookieManager.clearGoogleLink(response);
        cookieManager.clearOauthReturn(response);
        response.sendRedirect(googleProperties.frontendUrl("/login?oauthError=1"));
    }
}
