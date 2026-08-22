package com.flashcardapp.dto;

import com.flashcardapp.entity.FlashcardSet;

import java.time.LocalDateTime;
import java.util.UUID;

public record FlashcardSetSummaryResponse(
        UUID id,
        String title,
        String description,
        FolderResponse folder,
        int cardCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FlashcardSetSummaryResponse from(FlashcardSet set) {
        return new FlashcardSetSummaryResponse(
                set.getId(),
                set.getTitle(),
                set.getDescription(),
                FolderResponse.from(set.getFolder()),
                set.getCards().size(),
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }
}
