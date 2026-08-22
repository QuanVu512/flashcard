package com.flashcardapp.service;

import com.flashcardapp.config.OtpProperties;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.OtpChallenge;
import com.flashcardapp.entity.OtpPurpose;
import com.flashcardapp.helper.exception.OtpRateLimitException;
import com.flashcardapp.repository.AppUserRepository;
import com.flashcardapp.repository.OtpChallengeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OtpService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OtpChallengeRepository otpChallengeRepository;
    private final AppUserRepository appUserRepository;
    private final UserService userService;
    private final OtpMailQueueService mailQueueService;
    private final OtpRequestPolicyService requestPolicyService;
    private final OtpProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpChallengeRepository otpChallengeRepository,
                      AppUserRepository appUserRepository,
                      UserService userService,
                      OtpMailQueueService mailQueueService,
                      OtpRequestPolicyService requestPolicyService,
                      OtpProperties properties) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.appUserRepository = appUserRepository;
        this.userService = userService;
        this.mailQueueService = mailQueueService;
        this.requestPolicyService = requestPolicyService;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = OtpRateLimitException.class)
    public OtpDispatch dispatch(AppUser requestedUser,
                                OtpPurpose purpose,
                                HttpServletRequest request) {
        LocalDateTime now = now();
        AppUser user = lockUser(requestedUser.getId());
        String clientKeyHash = requestPolicyService.clientKey(request);
        requestPolicyService.assertNotBlocked(user.getId(), clientKeyHash, now);

        OtpChallenge activeChallenge = otpChallengeRepository
                .findFirstByUserIdAndPurposeAndClientKeyHashAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId(),
                        purpose,
                        clientKeyHash
                )
                .orElse(null);
        if (activeChallenge != null && activeChallenge.getExpiresAt().isAfter(now)) {
            return response(
                    activeChallenge,
                    mailQueueService.quota(user.getId(), now).remainingSends(),
                    now
            );
        }
        if (activeChallenge != null) {
            activeChallenge.setConsumedAt(now);
            otpChallengeRepository.save(activeChallenge);
        }

        OtpMailQueueService.OtpSendQuota quota = mailQueueService.quota(user.getId(), now);
        if (quota.exhausted()) {
            throw requestPolicyService.block(
                    user.getId(),
                    clientKeyHash,
                    now,
                    quota.retryAfterSeconds()
            );
        }

        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId(UUID.randomUUID());
        challenge.setUser(user);
        challenge.setPurpose(purpose);
        challenge.setCodeHash(hash(challenge.getId(), code));
        challenge.setAttempts(0);
        challenge.setCreatedAt(now);
        challenge.setSentAt(now);
        challenge.setExpiresAt(now.plus(properties.expirationDuration()));
        challenge.setClientKeyHash(clientKeyHash);
        challenge.setResendAvailableAt(now.plus(properties.initialResendDuration()));
        otpChallengeRepository.save(challenge);

        mailQueueService.enqueueInitial(challenge, code, now);
        return response(challenge, quota.remainingSends() - 1, now);
    }

    @Transactional(noRollbackFor = OtpRateLimitException.class)
    public OtpDispatch resend(UUID challengeId, HttpServletRequest request) {
        OtpChallenge existing = otpChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên OTP không tồn tại"));
        AppUser user = lockUser(existing.getUser().getId());
        OtpChallenge challenge = lockedChallenge(challengeId);
        LocalDateTime now = now();
        String clientKeyHash = requestPolicyService.clientKey(request);
        assertSameClient(challenge, clientKeyHash);
        requestPolicyService.assertNotBlocked(user.getId(), clientKeyHash, now);

        if (challenge.getConsumedAt() != null || !challenge.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("OTP đã hết hạn, vui lòng đăng nhập lại");
        }
        if (challenge.getResendAvailableAt() != null
                && challenge.getResendAvailableAt().isAfter(now)) {
            throw requestPolicyService.block(
                    user.getId(),
                    clientKeyHash,
                    now,
                    properties.browserBlockDuration().toSeconds()
            );
        }

        OtpMailQueueService.OtpSendQuota quota = mailQueueService.quota(user.getId(), now);
        if (quota.exhausted()) {
            throw requestPolicyService.block(
                    user.getId(),
                    clientKeyHash,
                    now,
                    quota.retryAfterSeconds()
            );
        }

        challenge.setSentAt(now);
        challenge.setExpiresAt(now.plus(properties.expirationDuration()));
        challenge.setResendAvailableAt(now.plus(properties.resendDuration()));
        otpChallengeRepository.save(challenge);
        mailQueueService.enqueueResend(challenge, now);
        return response(challenge, quota.remainingSends() - 1, now);
    }

    @Transactional(noRollbackFor = OtpRateLimitException.class)
    public AppUser verify(UUID challengeId, String code, HttpServletRequest request) {
        OtpChallenge existing = otpChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên OTP không tồn tại"));
        AppUser lockedUser = lockUser(existing.getUser().getId());
        OtpChallenge challenge = lockedChallenge(challengeId);
        LocalDateTime now = now();
        String clientKeyHash = requestPolicyService.clientKey(request);
        assertSameClient(challenge, clientKeyHash);
        requestPolicyService.assertNotBlocked(lockedUser.getId(), clientKeyHash, now);
        if (challenge.getConsumedAt() != null || !challenge.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("OTP đã hết hạn, vui lòng yêu cầu mã mới");
        }
        if (challenge.getAttempts() >= properties.maxAttemptsOrDefault()) {
            throw new IllegalArgumentException("OTP đã bị khóa do nhập sai quá nhiều lần");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        byte[] actual = hash(challenge.getId(), code).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(actual, expected)) {
            if (challenge.getAttempts() >= properties.maxAttemptsOrDefault()) {
                challenge.setConsumedAt(now);
            }
            otpChallengeRepository.save(challenge);
            throw new IllegalArgumentException("OTP chưa đúng");
        }

        challenge.setConsumedAt(now);
        otpChallengeRepository.save(challenge);
        AppUser user = challenge.getUser();
        if (challenge.getPurpose() == OtpPurpose.EMAIL_VERIFICATION && !user.isEmailVerified()) {
            userService.markEmailVerified(user);
        }
        return user;
    }

    private AppUser lockUser(UUID userId) {
        return appUserRepository.findForUpdateById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không còn tồn tại"));
    }

    private void assertSameClient(OtpChallenge challenge, String clientKeyHash) {
        if (challenge.getClientKeyHash() == null) {
            challenge.setClientKeyHash(clientKeyHash);
            return;
        }
        boolean matches = MessageDigest.isEqual(
                challenge.getClientKeyHash().getBytes(StandardCharsets.US_ASCII),
                clientKeyHash.getBytes(StandardCharsets.US_ASCII)
        );
        if (!matches) {
            throw new IllegalArgumentException("Phiên OTP không hợp lệ trên thiết bị này");
        }
    }

    private OtpChallenge lockedChallenge(UUID challengeId) {
        return otpChallengeRepository.findForUpdate(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên OTP không tồn tại"));
    }

    private OtpDispatch response(OtpChallenge challenge, int remainingSends, LocalDateTime now) {
        long expiresIn = Math.max(1, secondsUntil(now, challenge.getExpiresAt()));
        long resendAvailableIn = challenge.getResendAvailableAt() == null
                ? 0
                : secondsUntil(now, challenge.getResendAvailableAt());
        return new OtpDispatch(
                challenge.getId(),
                maskEmail(challenge.getUser().getEmail()),
                expiresIn,
                resendAvailableIn,
                Math.max(0, remainingSends)
        );
    }

    private String hash(UUID challengeId, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.requiredHashSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            byte[] digest = mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể bảo vệ OTP", exception);
        }
    }

    private long secondsUntil(LocalDateTime now, LocalDateTime target) {
        long millis = Duration.between(now, target).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(0, at));
        return email.charAt(0) + "***" + email.substring(at);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record OtpDispatch(
            UUID challengeId,
            String maskedEmail,
            long expiresInSeconds,
            long resendAvailableInSeconds,
            int remainingSends
    ) {
    }
}
