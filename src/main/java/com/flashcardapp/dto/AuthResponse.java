package com.flashcardapp.dto;

public record AuthResponse(
        long expiresInSeconds,
        UserProfileResponse user
) {
}
