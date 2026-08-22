package com.flashcardapp.dto;

import java.util.List;

public record LibraryResponse(
        UserProfileResponse user,
        List<FolderResponse> folders,
        List<FlashcardSetSummaryResponse> sets
) {
}
