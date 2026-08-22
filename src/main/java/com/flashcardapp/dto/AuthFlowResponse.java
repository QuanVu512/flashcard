package com.flashcardapp.dto;

import java.util.UUID;

public record AuthFlowResponse(
        String status,
        UUID challengeId,
        String maskedEmail,
        long expiresInSeconds,
        long resendAvailableInSeconds,
        int remainingSends,
        AuthResponse session
) {

    public static AuthFlowResponse otpRequired(UUID challengeId,
                                               String maskedEmail,
                                               long expiresInSeconds,
                                               long resendAvailableInSeconds,
                                               int remainingSends) {
        return new AuthFlowResponse(
                "OTP_REQUIRED",
                challengeId,
                maskedEmail,
                expiresInSeconds,
                resendAvailableInSeconds,
                remainingSends,
                null
        );
    }

    public static AuthFlowResponse authenticated(AuthResponse session) {
        return new AuthFlowResponse("AUTHENTICATED", null, null, 0, 0, 0, session);
    }
}
