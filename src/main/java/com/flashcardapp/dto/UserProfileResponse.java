package com.flashcardapp.dto;

import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.Client;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        boolean emailVerified,
        long score
) {

    public static UserProfileResponse from(AppUser user) {
        Client client = user.getClient();
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                client.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.isEmailVerified(),
                client.getScore()
        );
    }
}
