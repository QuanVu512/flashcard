package com.flashcardapp.helper.security;

import com.flashcardapp.config.GoogleAuthProperties;
import com.flashcardapp.entity.AuthMethod;
import com.flashcardapp.service.AuthSessionService;
import com.flashcardapp.service.GoogleIdentityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_RETURN_PATH = "/library";
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final GoogleIdentityService googleIdentityService;
    private final AuthSessionService authSessionService;
    private final JwtService jwtService;
    private final AuthCookieManager cookieManager;
    private final GoogleAuthProperties googleProperties;

    public GoogleOAuthSuccessHandler(GoogleIdentityService googleIdentityService,
                                     AuthSessionService authSessionService,
                                     JwtService jwtService,
                                     AuthCookieManager cookieManager,
                                     GoogleAuthProperties googleProperties) {
        this.googleIdentityService = googleIdentityService;
        this.authSessionService = authSessionService;
        this.jwtService = jwtService;
        this.cookieManager = cookieManager;
        this.googleProperties = googleProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            redirectFailure(request, response);
            return;
        }

        try {
            GoogleIdentityService.GoogleLoginResult result = googleIdentityService.resolve(oidcUser);
            invalidateTemporarySession(request);
            if (result.linkRequired()) {
                GoogleIdentityService.GoogleProfile profile = result.profile();
                String linkToken = jwtService.generateGoogleLinkToken(
                        result.user(),
                        profile.issuer(),
                        profile.subject(),
                        profile.email(),
                        profile.displayName()
                );
                cookieManager.writeGoogleLink(response, linkToken, Duration.ofMinutes(10));
                response.sendRedirect(googleProperties.frontendUrl(
                        "/login?googleLink=1&returnTo="
                                + URLEncoder.encode(oauthReturnPath(request), StandardCharsets.UTF_8)
                ));
                return;
            }

            authSessionService.issueSession(result.user(), AuthMethod.GOOGLE, response);
            cookieManager.clearGoogleLink(response);
            String returnPath = oauthReturnPath(request);
            cookieManager.clearOauthReturn(response);
            response.sendRedirect(googleProperties.frontendUrl(returnPath));
        } catch (RuntimeException exception) {
            LOGGER.warn("Google authentication could not create an application session", exception);
            redirectFailure(request, response);
        }
    }

    private boolean isSafeReturnPath(String path) {
        return path.startsWith("/")
                && !path.startsWith("//")
                && !path.startsWith("/\\")
                && !path.contains("\r")
                && !path.contains("\n");
    }

    private String oauthReturnPath(HttpServletRequest request) {
        return cookieManager.readOauthReturn(request)
                .filter(this::isSafeReturnPath)
                .orElse(DEFAULT_RETURN_PATH);
    }

    private void invalidateTemporarySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // The OAuth session may already have been invalidated in the success path.
        }
    }

    private void redirectFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invalidateTemporarySession(request);
        cookieManager.clearGoogleLink(response);
        cookieManager.clearOauthReturn(response);
        response.sendRedirect(googleProperties.frontendUrl("/login?oauthError=1"));
    }
}
