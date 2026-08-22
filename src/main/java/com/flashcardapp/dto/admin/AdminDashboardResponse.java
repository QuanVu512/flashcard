package com.flashcardapp.dto.admin;

import java.util.List;

public record AdminDashboardResponse(
        AdminStats stats,
        List<AdminUserRow> users
) {
}
