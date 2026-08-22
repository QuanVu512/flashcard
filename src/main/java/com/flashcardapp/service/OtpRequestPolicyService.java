package com.flashcardapp.service;

import com.flashcardapp.config.OtpProperties;
import com.flashcardapp.entity.OtpBrowserBlock;
import com.flashcardapp.helper.exception.OtpRateLimitException;
import com.flashcardapp.helper.security.SecureTokenService;
import com.flashcardapp.repository.OtpBrowserBlockRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OtpRequestPolicyService {

    private final OtpBrowserBlockRepository blockRepository;
    private final SecureTokenService secureTokenService;
    private final OtpProperties properties;

    public OtpRequestPolicyService(OtpBrowserBlockRepository blockRepository,
                                   SecureTokenService secureTokenService,
                                   OtpProperties properties) {
        this.blockRepository = blockRepository;
        this.secureTokenService = secureTokenService;
        this.properties = properties;
    }

    public String clientKey(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String source = request.getRemoteAddr() + "|" + (userAgent == null ? "unknown" : userAgent);
        return secureTokenService.hash(source);
    }

    public String userSubjectKey(UUID userId) {
        return secureTokenService.hash("user:" + userId);
    }

    public String registrationSubjectKey(String normalizedEmail) {
        return secureTokenService.hash("registration:" + normalizedEmail);
    }

    public void assertNotBlocked(String subjectKeyHash, String clientKeyHash, LocalDateTime now) {
        blockRepository.findForUpdate(subjectKeyHash, clientKeyHash).ifPresent(block -> {
            if (block.getBlockedUntil().isAfter(now)) {
                throw new OtpRateLimitException(
                        "Vui lòng thử lại sau.",
                        secondsUntil(now, block.getBlockedUntil()),
                        true
                );
            }
            blockRepository.delete(block);
        });
    }

    public OtpRateLimitException block(String subjectKeyHash,
                                       String clientKeyHash,
                                       LocalDateTime now,
                                       long retryAfterSeconds) {
        OtpBrowserBlock block = blockRepository.findForUpdate(subjectKeyHash, clientKeyHash)
                .orElseGet(() -> newBlock(subjectKeyHash, clientKeyHash));
        block.setBlockedUntil(now.plus(properties.browserBlockDuration()));
        block.setUpdatedAt(now);
        blockRepository.save(block);
        return new OtpRateLimitException("Vui lòng thử lại sau.", retryAfterSeconds, true);
    }

    @Transactional
    public void cleanupExpired(LocalDateTime now) {
        blockRepository.deleteByBlockedUntilLessThanEqual(now);
    }

    private OtpBrowserBlock newBlock(String subjectKeyHash, String clientKeyHash) {
        OtpBrowserBlock block = new OtpBrowserBlock();
        block.setId(UUID.randomUUID());
        block.setSubjectKeyHash(subjectKeyHash);
        block.setClientKeyHash(clientKeyHash);
        return block;
    }

    private long secondsUntil(LocalDateTime now, LocalDateTime target) {
        return Math.max(1, now.until(target, ChronoUnit.SECONDS));
    }
}
