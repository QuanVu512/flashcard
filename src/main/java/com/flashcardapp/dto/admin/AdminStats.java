package com.flashcardapp.dto.admin;

public record AdminStats(
        long userCount,
        long folderCount,
        long setCount,
        long cardCount,
        long totalScore
) {
}
