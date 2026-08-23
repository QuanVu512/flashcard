package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.auth.mail")
public class AuthMailProperties {

    private boolean enabled;
    private String from;
    private long workerDelayMs = 500;
    private int workerConcurrency = 2;
    private int deliveryMaxAttempts = 3;
    private long processingLeaseSeconds = 60;

    public String requiredFrom() {
        if (!enabled) {
            throw new IllegalStateException("Gửi OTP đang tắt. Hãy cấu hình AUTH_MAIL_ENABLED=true");
        }
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Cần cấu hình AUTH_MAIL_FROM");
        }
        return from.trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long workerDelayMsOrDefault() {
        return Math.max(100, workerDelayMs);
    }

    public int workerConcurrencyOrDefault() {
        return Math.max(1, Math.min(8, workerConcurrency));
    }

    public int deliveryMaxAttemptsOrDefault() {
        return Math.max(1, deliveryMaxAttempts);
    }

    public long processingLeaseSecondsOrDefault() {
        return Math.max(30, processingLeaseSeconds);
    }

    public long getWorkerDelayMs() {
        return workerDelayMs;
    }

    public void setWorkerDelayMs(long workerDelayMs) {
        this.workerDelayMs = workerDelayMs;
    }

    public int getWorkerConcurrency() {
        return workerConcurrency;
    }

    public void setWorkerConcurrency(int workerConcurrency) {
        this.workerConcurrency = workerConcurrency;
    }

    public int getDeliveryMaxAttempts() {
        return deliveryMaxAttempts;
    }

    public void setDeliveryMaxAttempts(int deliveryMaxAttempts) {
        this.deliveryMaxAttempts = deliveryMaxAttempts;
    }

    public long getProcessingLeaseSeconds() {
        return processingLeaseSeconds;
    }

    public void setProcessingLeaseSeconds(long processingLeaseSeconds) {
        this.processingLeaseSeconds = processingLeaseSeconds;
    }
}
