package com.flashcardapp.dto;

import com.flashcardapp.entity.FlashcardSet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FlashcardSetDetailResponse(
        UUID id,
        String title,
        String description,
        FolderResponse folder,
        List<StudyCardView> cards,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FlashcardSetDetailResponse from(FlashcardSet set, List<StudyCardView> cards) {
        return new FlashcardSetDetailResponse(
                set.getId(),
                set.getTitle(),
                set.getDescription(),
                FolderResponse.from(set.getFolder()),
                cards,
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }
}
