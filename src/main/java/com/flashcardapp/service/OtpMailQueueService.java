package com.flashcardapp.service;

import com.flashcardapp.config.AuthMailProperties;
import com.flashcardapp.config.OtpProperties;
import com.flashcardapp.entity.OtpChallenge;
import com.flashcardapp.entity.OtpMailDelivery;
import com.flashcardapp.entity.OtpMailDeliveryStatus;
import com.flashcardapp.entity.OtpPurpose;
import com.flashcardapp.helper.security.OtpCodeCipher;
import com.flashcardapp.repository.OtpMailDeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpMailQueueService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final OtpMailDeliveryRepository deliveryRepository;
    private final OtpCodeCipher codeCipher;
    private final OtpProperties otpProperties;
    private final AuthMailProperties mailProperties;

    public OtpMailQueueService(OtpMailDeliveryRepository deliveryRepository,
                               OtpCodeCipher codeCipher,
                               OtpProperties otpProperties,
                               AuthMailProperties mailProperties) {
        this.deliveryRepository = deliveryRepository;
        this.codeCipher = codeCipher;
        this.otpProperties = otpProperties;
        this.mailProperties = mailProperties;
    }

    @Transactional
    public void enqueueInitial(OtpChallenge challenge, String code, LocalDateTime now) {
        mailProperties.requiredFrom();
        saveDelivery(
                challenge,
                codeCipher.encrypt(challenge.getId(), code),
                now
        );
    }

    @Transactional
    public void enqueueResend(OtpChallenge challenge, LocalDateTime now) {
        mailProperties.requiredFrom();
        OtpMailDelivery original = deliveryRepository
                .findFirstByChallenge_IdOrderByCreatedAtAsc(challenge.getId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy nội dung OTP để gửi lại"));
        saveDelivery(challenge, original.getEncryptedCode(), now);
    }

    @Transactional(readOnly = true)
    public OtpSendQuota quota(String quotaKeyHash, LocalDateTime now) {
        LocalDateTime windowStart = now.minus(otpProperties.sendWindowDuration());
        long sentRequests = deliveryRepository
                .countByQuotaKeyHashAndCreatedAtGreaterThanEqual(quotaKeyHash, windowStart);
        int countedRequests = (int) Math.min(Integer.MAX_VALUE, sentRequests);
        int remainingSends = Math.max(0, otpProperties.maxSendsOrDefault() - countedRequests);
        if (remainingSends > 0) {
            return new OtpSendQuota(remainingSends, 0);
        }

        LocalDateTime oldestRequest = deliveryRepository
                .findFirstByQuotaKeyHashAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                        quotaKeyHash,
                        windowStart
                )
                .map(OtpMailDelivery::getCreatedAt)
                .orElse(now);
        LocalDateTime resetAt = oldestRequest.plus(otpProperties.sendWindowDuration());
        return new OtpSendQuota(0, Math.max(1, now.until(resetAt, ChronoUnit.SECONDS)));
    }

    @Transactional
    public Optional<ClaimedOtpMail> claimNext() {
        LocalDateTime now = now();
        return deliveryRepository.findReadyForUpdate(now)
                .map(delivery -> claim(delivery, now));
    }

    @Transactional
    public void markSent(UUID deliveryId) {
        OtpMailDelivery delivery = deliveryRepository.findForUpdate(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy email OTP đang gửi"));
        delivery.setStatus(OtpMailDeliveryStatus.SENT);
        delivery.setSentAt(now());
        delivery.setLastError(null);
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void markFailed(UUID deliveryId, Exception exception) {
        OtpMailDelivery delivery = deliveryRepository.findForUpdate(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy email OTP đang gửi"));
        delivery.setLastError(summarize(exception));
        if (delivery.getAttempts() >= mailProperties.deliveryMaxAttemptsOrDefault()) {
            delivery.setStatus(OtpMailDeliveryStatus.FAILED);
        } else {
            delivery.setStatus(OtpMailDeliveryStatus.PENDING);
            delivery.setAvailableAt(now().plusSeconds(retryDelaySeconds(delivery.getAttempts())));
        }
        deliveryRepository.save(delivery);
    }

    private ClaimedOtpMail claim(OtpMailDelivery delivery, LocalDateTime now) {
        delivery.setStatus(OtpMailDeliveryStatus.PROCESSING);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setAvailableAt(now.plusSeconds(mailProperties.processingLeaseSecondsOrDefault()));
        deliveryRepository.save(delivery);
        return new ClaimedOtpMail(
                delivery.getId(),
                delivery.getRecipient(),
                codeCipher.decrypt(delivery.getChallenge().getId(), delivery.getEncryptedCode()),
                delivery.getPurpose(),
                otpProperties.getExpirationMinutes()
        );
    }

    private void saveDelivery(OtpChallenge challenge, String encryptedCode, LocalDateTime now) {
        OtpMailDelivery delivery = new OtpMailDelivery();
        delivery.setId(UUID.randomUUID());
        delivery.setChallenge(challenge);
        delivery.setUserId(challenge.getUser() == null ? null : challenge.getUser().getId());
        delivery.setQuotaKeyHash(challenge.getSubjectKeyHash());
        delivery.setRecipient(challenge.recipientEmail());
        delivery.setPurpose(challenge.getPurpose());
        delivery.setEncryptedCode(encryptedCode);
        delivery.setStatus(OtpMailDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setAvailableAt(now);
        delivery.setCreatedAt(now);
        deliveryRepository.save(delivery);
    }

    private long retryDelaySeconds(int attempts) {
        return Math.min(300, 5L << Math.min(5, Math.max(0, attempts - 1)));
    }

    private String summarize(Exception exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public record OtpSendQuota(int remainingSends, long retryAfterSeconds) {
        public boolean exhausted() {
            return remainingSends <= 0;
        }
    }

    public record ClaimedOtpMail(
            UUID deliveryId,
            String recipient,
            String code,
            OtpPurpose purpose,
            long expirationMinutes
    ) {
    }
}
