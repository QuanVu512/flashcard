package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.session")
public class AuthSessionProperties {

    private long accessMinutes = 15;
    private long passwordDays = 3;
    private long googleDays = 30;
    private long trustedDeviceDays = 30;

    public Duration accessDuration() {
        return Duration.ofMinutes(Math.max(1, accessMinutes));
    }

    public Duration passwordDuration() {
        return Duration.ofDays(Math.max(1, passwordDays));
    }

    public Duration googleDuration() {
        return Duration.ofDays(Math.max(1, googleDays));
    }

    public Duration trustedDeviceDuration() {
        return Duration.ofDays(Math.max(1, trustedDeviceDays));
    }

    public long getAccessMinutes() {
        return accessMinutes;
    }

    public void setAccessMinutes(long accessMinutes) {
        this.accessMinutes = accessMinutes;
    }

    public long getPasswordDays() {
        return passwordDays;
    }

    public void setPasswordDays(long passwordDays) {
        this.passwordDays = passwordDays;
    }

    public long getGoogleDays() {
        return googleDays;
    }

    public void setGoogleDays(long googleDays) {
        this.googleDays = googleDays;
    }

    public long getTrustedDeviceDays() {
        return trustedDeviceDays;
    }

    public void setTrustedDeviceDays(long trustedDeviceDays) {
        this.trustedDeviceDays = trustedDeviceDays;
    }
}
