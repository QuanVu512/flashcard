package com.flashcardapp.controller;

import com.flashcardapp.dto.TranslationSuggestionResponse;
import com.flashcardapp.service.TranslationSuggestionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
public class TranslationSuggestionController {

    private final TranslationSuggestionService translationSuggestionService;

    public TranslationSuggestionController(TranslationSuggestionService translationSuggestionService) {
        this.translationSuggestionService = translationSuggestionService;
    }

    @GetMapping("/api/translation/suggest")
    public TranslationSuggestionResponse suggest(@RequestParam @NotBlank @Size(max = 240) String text,
                                                 @RequestParam(required = false) @Size(max = 16) String source,
                                                 @RequestParam(required = false) @Size(max = 16) String target) {
        try {
            return translationSuggestionService.suggest(text, source, target);
        } catch (Exception ignored) {
            return TranslationSuggestionResponse.disabled("Gợi ý dịch đang gặp lỗi. Kiểm tra cấu hình Azure rồi thử lại.");
        }
    }
}
