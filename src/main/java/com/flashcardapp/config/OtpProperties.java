package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.otp")
public class OtpProperties {

    private long expirationMinutes = 5;
    private long initialResendSeconds = 10;
    private long resendSeconds = 60;
    private int maxAttempts = 5;
    private int maxSends = 5;
    private long sendWindowHours = 6;
    private long browserBlockSeconds = 120;
    private String hashSecret;

    public Duration expirationDuration() {
        return Duration.ofMinutes(Math.max(1, expirationMinutes));
    }

    public Duration resendDuration() {
        return Duration.ofSeconds(Math.max(15, resendSeconds));
    }

    public Duration initialResendDuration() {
        return Duration.ofSeconds(Math.max(1, initialResendSeconds));
    }

    public Duration sendWindowDuration() {
        return Duration.ofHours(Math.max(1, sendWindowHours));
    }

    public Duration browserBlockDuration() {
        return Duration.ofSeconds(Math.max(30, browserBlockSeconds));
    }

    public int maxAttemptsOrDefault() {
        return Math.max(1, maxAttempts);
    }

    public int maxSendsOrDefault() {
        return Math.max(1, maxSends);
    }

    public String requiredHashSecret() {
        if (!StringUtils.hasText(hashSecret) || hashSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("AUTH_OTP_HASH_SECRET cần tối thiểu 32 bytes");
        }
        return hashSecret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long getResendSeconds() {
        return resendSeconds;
    }

    public void setResendSeconds(long resendSeconds) {
        this.resendSeconds = resendSeconds;
    }

    public long getInitialResendSeconds() {
        return initialResendSeconds;
    }

    public void setInitialResendSeconds(long initialResendSeconds) {
        this.initialResendSeconds = initialResendSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxSends() {
        return maxSends;
    }

    public void setMaxSends(int maxSends) {
        this.maxSends = maxSends;
    }

    public long getSendWindowHours() {
        return sendWindowHours;
    }

    public void setSendWindowHours(long sendWindowHours) {
        this.sendWindowHours = sendWindowHours;
    }

    public long getBrowserBlockSeconds() {
        return browserBlockSeconds;
    }

    public void setBrowserBlockSeconds(long browserBlockSeconds) {
        this.browserBlockSeconds = browserBlockSeconds;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }
}
