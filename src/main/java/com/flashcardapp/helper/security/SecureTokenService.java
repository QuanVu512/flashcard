package com.flashcardapp.helper.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecureTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenMaterial createToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new TokenMaterial(rawToken, hash(rawToken));
    }

    public String encode(UUID selector, String rawToken) {
        return selector + "." + rawToken;
    }

    public Optional<ParsedToken> parse(String encodedToken) {
        if (encodedToken == null) return Optional.empty();
        int separator = encodedToken.indexOf('.');
        if (separator <= 0 || separator == encodedToken.length() - 1) return Optional.empty();
        try {
            return Optional.of(new ParsedToken(
                    UUID.fromString(encodedToken.substring(0, separator)),
                    encodedToken.substring(separator + 1)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    public boolean matches(String rawToken, String expectedHash) {
        return expectedHash != null && MessageDigest.isEqual(
                hash(rawToken).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    public record TokenMaterial(String rawToken, String hash) {
    }

    public record ParsedToken(UUID selector, String rawToken) {
    }
}
