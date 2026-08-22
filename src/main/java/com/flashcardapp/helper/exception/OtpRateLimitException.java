package com.flashcardapp.helper.exception;

public class OtpRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;
    private final boolean redirectToLogin;

    public OtpRateLimitException(String message, long retryAfterSeconds, boolean redirectToLogin) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
        this.redirectToLogin = redirectToLogin;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public boolean isRedirectToLogin() {
        return redirectToLogin;
    }
}
