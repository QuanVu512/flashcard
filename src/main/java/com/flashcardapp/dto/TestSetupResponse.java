package com.flashcardapp.dto;

public record TestSetupResponse(
        FlashcardSetSummaryResponse set,
        int maxQuestions,
        int defaultQuestions,
        int defaultMinutes,
        String mode
) {
}
