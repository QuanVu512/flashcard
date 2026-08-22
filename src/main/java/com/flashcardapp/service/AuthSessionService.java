package com.flashcardapp.service;

import com.flashcardapp.config.AuthSessionProperties;
import com.flashcardapp.dto.AuthResponse;
import com.flashcardapp.dto.UserProfileResponse;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.AuthMethod;
import com.flashcardapp.entity.RefreshSession;
import com.flashcardapp.helper.security.AuthCookieManager;
import com.flashcardapp.helper.security.JwtService;
import com.flashcardapp.helper.security.SecureTokenService;
import com.flashcardapp.repository.RefreshSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthSessionService {

    private final RefreshSessionRepository refreshSessionRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthCookieManager cookieManager;
    private final SecureTokenService secureTokenService;
    private final AuthSessionProperties properties;

    public AuthSessionService(RefreshSessionRepository refreshSessionRepository,
                              UserService userService,
                              JwtService jwtService,
                              AuthCookieManager cookieManager,
                              SecureTokenService secureTokenService,
                              AuthSessionProperties properties) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.cookieManager = cookieManager;
        this.secureTokenService = secureTokenService;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse issueSession(AppUser user, AuthMethod authMethod, HttpServletResponse response) {
        LocalDateTime expiresAt = now().plus(sessionDuration(authMethod));
        return issueSession(user, authMethod, expiresAt, response);
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        SecureTokenService.ParsedToken parsedToken = cookieManager.readRefreshToken(request)
                .flatMap(secureTokenService::parse)
                .orElseThrow(() -> expiredSession(response));

        RefreshSession current = refreshSessionRepository.findForUpdate(parsedToken.selector())
                .orElseThrow(() -> expiredSession(response));
        LocalDateTime now = now();
        if (current.getRevokedAt() != null) {
            throw new BadCredentialsException("Refresh token đã được sử dụng");
        }
        if (!current.getExpiresAt().isAfter(now)
                || !secureTokenService.matches(parsedToken.rawToken(), current.getTokenHash())) {
            throw expiredSession(response);
        }

        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        refreshSessionRepository.save(current);
        return issueSession(current.getUser(), current.getAuthMethod(), current.getExpiresAt(), response);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieManager.readRefreshToken(request)
                .flatMap(secureTokenService::parse)
                .flatMap(token -> refreshSessionRepository.findById(token.selector()))
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(now());
                        refreshSessionRepository.save(session);
                    }
                });
        cookieManager.clearAccessToken(response);
        cookieManager.clearRefreshToken(response);
    }

    private AuthResponse issueSession(AppUser user,
                                      AuthMethod authMethod,
                                      LocalDateTime expiresAt,
                                      HttpServletResponse response) {
        if (!user.isEnabled()) {
            cookieManager.clearAccessToken(response);
            cookieManager.clearRefreshToken(response);
            throw new DisabledException("Tài khoản đã bị khóa");
        }
        LocalDateTime now = now();
        if (!expiresAt.isAfter(now)) throw expiredSession(response);

        SecureTokenService.TokenMaterial material = secureTokenService.createToken();
        RefreshSession refreshSession = new RefreshSession();
        refreshSession.setId(UUID.randomUUID());
        refreshSession.setUser(user);
        refreshSession.setTokenHash(material.hash());
        refreshSession.setAuthMethod(authMethod);
        refreshSession.setExpiresAt(expiresAt);
        refreshSession.setCreatedAt(now);
        refreshSession.setLastUsedAt(now);
        refreshSessionRepository.save(refreshSession);

        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        long accessSeconds = jwtService.expirationSeconds();
        long sessionSeconds = Math.max(1, now.until(expiresAt, ChronoUnit.SECONDS));
        cookieManager.writeAccessToken(
                response,
                jwtService.generateAccessToken(userDetails, authMethod),
                accessSeconds
        );
        cookieManager.writeRefreshToken(
                response,
                secureTokenService.encode(refreshSession.getId(), material.rawToken()),
                Duration.ofSeconds(sessionSeconds)
        );
        return new AuthResponse(accessSeconds, sessionSeconds, UserProfileResponse.from(user));
    }

    private Duration sessionDuration(AuthMethod authMethod) {
        return authMethod == AuthMethod.GOOGLE
                ? properties.googleDuration()
                : properties.passwordDuration();
    }

    private BadCredentialsException expiredSession(HttpServletResponse response) {
        cookieManager.clearAccessToken(response);
        cookieManager.clearRefreshToken(response);
        return new BadCredentialsException("Phiên đăng nhập đã hết hạn");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
