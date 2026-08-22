package com.flashcardapp.service;

import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.AuthIdentity;
import com.flashcardapp.entity.AuthIdentityProvider;
import com.flashcardapp.entity.Client;
import com.flashcardapp.repository.AppUserRepository;
import com.flashcardapp.repository.AuthIdentityRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class GoogleIdentityService {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private final AuthIdentityRepository authIdentityRepository;
    private final AppUserRepository appUserRepository;
    private final UserService userService;
    private final PendingRegistrationService pendingRegistrationService;

    public GoogleIdentityService(AuthIdentityRepository authIdentityRepository,
                                 AppUserRepository appUserRepository,
                                 UserService userService,
                                 PendingRegistrationService pendingRegistrationService) {
        this.authIdentityRepository = authIdentityRepository;
        this.appUserRepository = appUserRepository;
        this.userService = userService;
        this.pendingRegistrationService = pendingRegistrationService;
    }

    @Transactional
    public GoogleLoginResult resolve(OidcUser oidcUser) {
        GoogleProfile profile = profile(oidcUser);
        pendingRegistrationService.invalidateActive(profile.email());
        AuthIdentity existingIdentity = authIdentityRepository
                .findByProviderAndIssuerAndSubject(
                        AuthIdentityProvider.GOOGLE,
                        profile.issuer(),
                        profile.subject()
                )
                .orElse(null);
        if (existingIdentity != null) {
            AppUser user = existingIdentity.getUser();
            if (!user.isEmailVerified()) userService.markEmailVerified(user);
            return GoogleLoginResult.authenticated(user, profile);
        }

        AppUser existingUser = appUserRepository.findWithClientByEmailIgnoreCase(profile.email()).orElse(null);
        if (existingUser != null) {
            return GoogleLoginResult.linkRequired(existingUser, profile);
        }

        Client client = new Client();
        client.setDisplayName(StringUtils.hasText(profile.displayName())
                ? profile.displayName().trim()
                : profile.email().substring(0, profile.email().indexOf('@')));

        AppUser user = new AppUser();
        user.setEmail(profile.email());
        user.setPasswordHash(null);
        user.setEmailVerified(true);
        user.setClient(client);
        AppUser savedUser = appUserRepository.save(user);
        createGoogleIdentity(savedUser, profile);
        return GoogleLoginResult.authenticated(savedUser, profile);
    }

    @Transactional
    public AppUser link(AppUser user, GoogleProfile profile) {
        if (!userService.normalizeEmail(user.getEmail()).equals(profile.email())) {
            throw new IllegalArgumentException("Email Google không khớp tài khoản cần liên kết");
        }

        AuthIdentity existing = authIdentityRepository
                .findByProviderAndIssuerAndSubject(
                        AuthIdentityProvider.GOOGLE,
                        profile.issuer(),
                        profile.subject()
                )
                .orElse(null);
        if (existing != null && !existing.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Tài khoản Google đã liên kết với người dùng khác");
        }
        if (existing == null) {
            if (authIdentityRepository.findByUserIdAndProvider(
                    user.getId(),
                    AuthIdentityProvider.GOOGLE
            ).isPresent()) {
                throw new IllegalStateException("Tài khoản này đã liên kết với một Google identity khác");
            }
            createGoogleIdentity(user, profile);
        }
        return userService.markEmailVerified(user);
    }

    private void createGoogleIdentity(AppUser user, GoogleProfile profile) {
        AuthIdentity identity = new AuthIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUser(user);
        identity.setProvider(AuthIdentityProvider.GOOGLE);
        identity.setIssuer(profile.issuer());
        identity.setSubject(profile.subject());
        identity.setProviderEmail(profile.email());
        identity.setEmailVerified(true);
        identity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        authIdentityRepository.save(identity);
    }

    private GoogleProfile profile(OidcUser oidcUser) {
        String issuer = oidcUser.getIdToken().getIssuer() == null
                ? null
                : oidcUser.getIdToken().getIssuer().toString();
        if ("accounts.google.com".equals(issuer)) issuer = GOOGLE_ISSUER;
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean verified = oidcUser.getClaimAsBoolean("email_verified");
        if (!GOOGLE_ISSUER.equals(issuer)
                || !StringUtils.hasText(subject)
                || !StringUtils.hasText(email)
                || !Boolean.TRUE.equals(verified)) {
            throw new IllegalArgumentException("Google không cung cấp danh tính email đã xác minh");
        }
        return new GoogleProfile(
                GOOGLE_ISSUER,
                subject,
                userService.normalizeEmail(email),
                oidcUser.getClaimAsString("name")
        );
    }

    public record GoogleProfile(String issuer, String subject, String email, String displayName) {
    }

    public record GoogleLoginResult(AppUser user, GoogleProfile profile, boolean linkRequired) {
        public static GoogleLoginResult authenticated(AppUser user, GoogleProfile profile) {
            return new GoogleLoginResult(user, profile, false);
        }

        public static GoogleLoginResult linkRequired(AppUser user, GoogleProfile profile) {
            return new GoogleLoginResult(user, profile, true);
        }
    }
}
