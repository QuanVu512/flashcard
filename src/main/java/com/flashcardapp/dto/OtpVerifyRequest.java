package com.flashcardapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OtpVerifyRequest(
        @NotNull(message = "Thiếu phiên xác thực OTP")
        UUID challengeId,

        @NotBlank(message = "Vui lòng nhập OTP")
        @Pattern(regexp = "\\d{6}", message = "OTP phải gồm đúng 6 chữ số")
        String code,

        boolean rememberDevice
) {
}
