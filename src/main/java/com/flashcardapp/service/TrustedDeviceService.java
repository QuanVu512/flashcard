package com.flashcardapp.service;

import com.flashcardapp.config.AuthSessionProperties;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.TrustedDevice;
import com.flashcardapp.helper.security.AuthCookieManager;
import com.flashcardapp.helper.security.SecureTokenService;
import com.flashcardapp.repository.TrustedDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TrustedDeviceService {

    private final TrustedDeviceRepository trustedDeviceRepository;
    private final SecureTokenService secureTokenService;
    private final AuthCookieManager cookieManager;
    private final AuthSessionProperties properties;

    public TrustedDeviceService(TrustedDeviceRepository trustedDeviceRepository,
                                SecureTokenService secureTokenService,
                                AuthCookieManager cookieManager,
                                AuthSessionProperties properties) {
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.secureTokenService = secureTokenService;
        this.cookieManager = cookieManager;
        this.properties = properties;
    }

    @Transactional
    public boolean isTrusted(AppUser user, HttpServletRequest request) {
        SecureTokenService.ParsedToken parsedToken = cookieManager.readTrustedDevice(request)
                .flatMap(secureTokenService::parse)
                .orElse(null);
        if (parsedToken == null) return false;

        TrustedDevice device = trustedDeviceRepository.findById(parsedToken.selector()).orElse(null);
        LocalDateTime now = now();
        if (device == null
                || device.getRevokedAt() != null
                || !device.getUser().getId().equals(user.getId())
                || !device.getExpiresAt().isAfter(now)
                || !secureTokenService.matches(parsedToken.rawToken(), device.getTokenHash())
                || !secureTokenService.hash(userAgent(request)).equals(device.getUserAgentHash())) {
            return false;
        }
        device.setLastUsedAt(now);
        trustedDeviceRepository.save(device);
        return true;
    }

    @Transactional
    public void remember(AppUser user, HttpServletRequest request, HttpServletResponse response) {
        SecureTokenService.TokenMaterial material = secureTokenService.createToken();
        LocalDateTime now = now();

        TrustedDevice device = new TrustedDevice();
        device.setId(UUID.randomUUID());
        device.setUser(user);
        device.setTokenHash(material.hash());
        device.setUserAgentHash(secureTokenService.hash(userAgent(request)));
        device.setCreatedAt(now);
        device.setLastUsedAt(now);
        device.setExpiresAt(now.plus(properties.trustedDeviceDuration()));
        trustedDeviceRepository.save(device);

        cookieManager.writeTrustedDevice(
                response,
                secureTokenService.encode(device.getId(), material.rawToken()),
                properties.trustedDeviceDuration()
        );
    }

    private String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? "unknown" : value;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
