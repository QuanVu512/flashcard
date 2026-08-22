package com.flashcardapp.service;

import com.flashcardapp.config.OtpProperties;
import com.flashcardapp.entity.OtpBrowserBlock;
import com.flashcardapp.helper.exception.OtpRateLimitException;
import com.flashcardapp.helper.security.SecureTokenService;
import com.flashcardapp.repository.OtpBrowserBlockRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

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

    public void assertNotBlocked(UUID userId, String clientKeyHash, LocalDateTime now) {
        blockRepository.findForUpdate(userId, clientKeyHash).ifPresent(block -> {
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

    public OtpRateLimitException block(UUID userId,
                                       String clientKeyHash,
                                       LocalDateTime now,
                                       long retryAfterSeconds) {
        OtpBrowserBlock block = blockRepository.findForUpdate(userId, clientKeyHash)
                .orElseGet(() -> newBlock(userId, clientKeyHash));
        block.setBlockedUntil(now.plus(properties.browserBlockDuration()));
        block.setUpdatedAt(now);
        blockRepository.save(block);
        return new OtpRateLimitException("Vui lòng thử lại sau.", retryAfterSeconds, true);
    }

    private OtpBrowserBlock newBlock(UUID userId, String clientKeyHash) {
        OtpBrowserBlock block = new OtpBrowserBlock();
        block.setId(UUID.randomUUID());
        block.setUserId(userId);
        block.setClientKeyHash(clientKeyHash);
        return block;
    }

    private long secondsUntil(LocalDateTime now, LocalDateTime target) {
        return Math.max(1, now.until(target, ChronoUnit.SECONDS));
    }
}
