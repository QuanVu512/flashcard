package com.flashcardapp.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserRow(
        UUID id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        LocalDateTime createdAt,
        long score
) {

    public boolean admin() {
        return "ROLE_ADMIN".equals(role);
    }
}
