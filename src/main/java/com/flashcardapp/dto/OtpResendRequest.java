package com.flashcardapp.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OtpResendRequest(
        @NotNull(message = "Thiếu phiên xác thực OTP")
        UUID challengeId
) {
}
