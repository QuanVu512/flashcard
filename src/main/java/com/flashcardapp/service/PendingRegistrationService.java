package com.flashcardapp.service;

import com.flashcardapp.config.OtpProperties;
import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.PendingRegistration;
import com.flashcardapp.repository.AppUserRepository;
import com.flashcardapp.repository.PendingRegistrationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class PendingRegistrationService {

    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final AppUserRepository appUserRepository;
    private final UserService userService;
    private final RegistrationEmailLockService registrationEmailLockService;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;

    public PendingRegistrationService(PendingRegistrationRepository pendingRegistrationRepository,
                                      AppUserRepository appUserRepository,
                                      UserService userService,
                                      RegistrationEmailLockService registrationEmailLockService,
                                      PasswordEncoder passwordEncoder,
                                      OtpProperties otpProperties) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.appUserRepository = appUserRepository;
        this.userService = userService;
        this.registrationEmailLockService = registrationEmailLockService;
        this.passwordEncoder = passwordEncoder;
        this.otpProperties = otpProperties;
    }

    @Transactional
    public PendingRegistration create(RegisterRequest request) {
        String email = userService.normalizeEmail(request.getEmail());
        String displayName = request.getDisplayName().trim();
        String passwordHash = passwordEncoder.encode(request.getPassword());
        LocalDateTime now = now();
        registrationEmailLockService.acquire(email, now);
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("Email đã được sử dụng");
        }

        PendingRegistration registration = new PendingRegistration();
        registration.setId(UUID.randomUUID());
        registration.setEmail(email);
        registration.setDisplayName(displayName);
        registration.setPasswordHash(passwordHash);
        registration.setCreatedAt(now);
        registration.setExpiresAt(now.plus(otpProperties.expirationDuration()));
        return pendingRegistrationRepository.save(registration);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PendingRegistration lockAttempt(String normalizedEmail,
                                           UUID registrationId,
                                           LocalDateTime now) {
        registrationEmailLockService.acquire(normalizedEmail, now);
        return activeAttempts(normalizedEmail, now).stream()
                .filter(registration -> registration.getId().equals(registrationId))
                .filter(registration -> registration.isActiveAt(now))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Yêu cầu đăng ký đã hết hạn hoặc không còn hiệu lực"
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AppUser complete(PendingRegistration requestedRegistration, LocalDateTime now) {
        PendingRegistration registration = lockAttempt(
                requestedRegistration.getEmail(),
                requestedRegistration.getId(),
                now
        );
        if (appUserRepository.existsByEmailIgnoreCase(registration.getEmail())) {
            throw new UserAlreadyExistsException("Yêu cầu đăng ký không còn hiệu lực");
        }

        try {
            AppUser user = userService.createVerifiedLocalUser(registration);
            markCompleted(activeAttempts(registration.getEmail(), now), now);
            return user;
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("Yêu cầu đăng ký không còn hiệu lực");
        }
    }

    @Transactional
    public void cancel(UUID registrationId) {
        pendingRegistrationRepository.findById(registrationId).ifPresent(registration -> {
            LocalDateTime now = now();
            registrationEmailLockService.acquire(registration.getEmail(), now);
            List<PendingRegistration> attempts = activeAttempts(registration.getEmail(), now);
            attempts.stream()
                    .filter(attempt -> attempt.getId().equals(registrationId))
                    .findFirst()
                    .ifPresent(attempt -> attempt.setCompletedAt(now));
        });
    }

    @Transactional
    public void invalidateActive(String email) {
        LocalDateTime now = now();
        String normalizedEmail = userService.normalizeEmail(email);
        registrationEmailLockService.acquire(normalizedEmail, now);
        markCompleted(activeAttempts(normalizedEmail, now), now);
    }

    @Transactional
    public int cleanupStale(LocalDateTime cutoff) {
        return pendingRegistrationRepository.deleteStale(cutoff);
    }

    private List<PendingRegistration> activeAttempts(String normalizedEmail, LocalDateTime now) {
        return pendingRegistrationRepository.findActiveByEmail(normalizedEmail, now);
    }

    private void markCompleted(List<PendingRegistration> registrations, LocalDateTime completedAt) {
        registrations.forEach(registration -> registration.setCompletedAt(completedAt));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
