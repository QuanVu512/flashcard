package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.auth.mail.mailjet")
public class MailjetMailProperties {

    private String apiKey;
    private String secretKey;
    private String baseUrl = "https://api.mailjet.com";
    private String senderName = "Flashcard";
    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 10_000;

    public String requiredApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Cần cấu hình MAILJET_API_KEY khi AUTH_MAIL_PROVIDER=mailjet");
        }
        return apiKey.trim();
    }

    public String requiredSecretKey() {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("Cần cấu hình MAILJET_SECRET_KEY khi AUTH_MAIL_PROVIDER=mailjet");
        }
        return secretKey.trim();
    }

    public String baseUrlOrDefault() {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.mailjet.com";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String senderNameOrDefault() {
        return StringUtils.hasText(senderName) ? senderName.trim() : "Flashcard";
    }

    public int connectTimeoutMsOrDefault() {
        return Math.max(1_000, connectTimeoutMs);
    }

    public int readTimeoutMsOrDefault() {
        return Math.max(1_000, readTimeoutMs);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
