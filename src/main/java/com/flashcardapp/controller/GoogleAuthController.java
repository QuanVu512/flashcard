package com.flashcardapp.controller;

import com.flashcardapp.config.GoogleAuthProperties;
import com.flashcardapp.dto.AuthResponse;
import com.flashcardapp.dto.GoogleLinkRequest;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.AuthMethod;
import com.flashcardapp.helper.security.AuthCookieManager;
import com.flashcardapp.helper.security.JwtService;
import com.flashcardapp.service.AuthSessionService;
import com.flashcardapp.service.GoogleIdentityService;
import com.flashcardapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleAuthProperties googleProperties;
    private final AuthCookieManager cookieManager;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final GoogleIdentityService googleIdentityService;
    private final AuthSessionService authSessionService;

    public GoogleAuthController(GoogleAuthProperties googleProperties,
                                AuthCookieManager cookieManager,
                                JwtService jwtService,
                                AuthenticationManager authenticationManager,
                                UserService userService,
                                GoogleIdentityService googleIdentityService,
                                AuthSessionService authSessionService) {
        this.googleProperties = googleProperties;
        this.cookieManager = cookieManager;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.googleIdentityService = googleIdentityService;
        this.authSessionService = authSessionService;
    }

    @GetMapping("/start")
    public void start(@RequestParam(defaultValue = "/library") String returnTo,
                      HttpServletResponse response) throws IOException {
        if (!googleProperties.isEnabled()) {
            response.sendRedirect(googleProperties.frontendUrl("/login?oauthError=notConfigured"));
            return;
        }
        String safeReturnPath = isSafeReturnPath(returnTo) ? returnTo : "/library";
        cookieManager.writeOauthReturn(response, safeReturnPath, Duration.ofMinutes(10));
        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/link")
    public AuthResponse link(@Valid @RequestBody GoogleLinkRequest request,
                             HttpServletRequest servletRequest,
                             HttpServletResponse response) {
        String token = cookieManager.readGoogleLink(servletRequest)
                .orElseThrow(() -> new IllegalArgumentException("Phiên liên kết Google đã hết hạn"));
        JwtService.GoogleLinkClaims claims = jwtService.readGoogleLinkToken(token);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                claims.localEmail(),
                request.password()
        ));

        AppUser user = userService.currentUser(claims.localEmail());
        AppUser linkedUser = googleIdentityService.link(
                user,
                new GoogleIdentityService.GoogleProfile(
                        claims.issuer(),
                        claims.subject(),
                        userService.normalizeEmail(claims.googleEmail()),
                        claims.displayName()
                )
        );
        cookieManager.clearGoogleLink(response);
        cookieManager.clearOauthReturn(response);
        return authSessionService.issueSession(linkedUser, AuthMethod.GOOGLE, response);
    }

    private boolean isSafeReturnPath(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && !path.startsWith("/\\")
                && !path.contains("\r")
                && !path.contains("\n");
    }
}
