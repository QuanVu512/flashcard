package com.flashcardapp.dto;

import java.util.List;

public record PracticeSessionResponse(
        FlashcardSetSummaryResponse set,
        String mode,
        String answerMode,
        int questionCount,
        int minutes,
        String questionLabel,
        List<PracticeQuestion> questions
) {
}
