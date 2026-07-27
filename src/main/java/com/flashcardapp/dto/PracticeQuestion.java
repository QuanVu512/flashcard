package com.flashcardapp.dto;

import java.util.List;

public record PracticeQuestion(String term, String correctAnswer, List<String> choices) {
}
