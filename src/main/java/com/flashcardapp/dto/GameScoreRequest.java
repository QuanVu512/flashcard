package com.flashcardapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GameScoreRequest(
        @Min(value = 1, message = "Điểm cần lớn hơn 0")
        @Max(value = 20000, message = "Điểm một lượt chơi vượt mức cho phép")
        long score
) {
}
