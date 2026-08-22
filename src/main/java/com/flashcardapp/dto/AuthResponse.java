package com.flashcardapp.dto;

public record AuthResponse(
        long accessExpiresInSeconds,
        long sessionExpiresInSeconds,
        UserProfileResponse user
) {
}
