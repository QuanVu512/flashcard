package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String base64Secret;
    private String secret;
    private long validityInSeconds;
    private long expirationMinutes = 120;

    public String getBase64Secret() {
        return base64Secret;
    }

    public void setBase64Secret(String base64Secret) {
        this.base64Secret = base64Secret;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getValidityInSeconds() {
        return validityInSeconds;
    }

    public void setValidityInSeconds(long validityInSeconds) {
        this.validityInSeconds = validityInSeconds;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public long validityInSeconds() {
        return validityInSeconds > 0 ? validityInSeconds : Math.max(1, expirationMinutes) * 60;
    }
}
