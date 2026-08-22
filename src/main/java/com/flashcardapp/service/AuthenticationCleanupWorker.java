package com.flashcardapp.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class AuthenticationCleanupWorker {

    private static final long CLEANUP_GRACE_SECONDS = 60;

    private final PendingRegistrationService pendingRegistrationService;
    private final OtpRequestPolicyService requestPolicyService;

    public AuthenticationCleanupWorker(PendingRegistrationService pendingRegistrationService,
                                       OtpRequestPolicyService requestPolicyService) {
        this.pendingRegistrationService = pendingRegistrationService;
        this.requestPolicyService = requestPolicyService;
    }

    @Scheduled(fixedDelayString = "${app.auth.registration.cleanup-delay-ms:60000}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        pendingRegistrationService.cleanupStale(now.minusSeconds(CLEANUP_GRACE_SECONDS));
        requestPolicyService.cleanupExpired(now);
    }
}
