package com.flashcardapp.controller;

import com.flashcardapp.dto.TranslationSuggestionResponse;
import com.flashcardapp.service.TranslationSuggestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranslationSuggestionController {

    private final TranslationSuggestionService translationSuggestionService;

    public TranslationSuggestionController(TranslationSuggestionService translationSuggestionService) {
        this.translationSuggestionService = translationSuggestionService;
    }

    @GetMapping("/api/translation/suggest")
    public TranslationSuggestionResponse suggest(@RequestParam String text,
                                                 @RequestParam(required = false) String source,
                                                 @RequestParam(required = false) String target) {
        try {
            return translationSuggestionService.suggest(text, source, target);
        } catch (Exception ignored) {
            return TranslationSuggestionResponse.disabled("Gợi ý dịch đang gặp lỗi. Kiểm tra cấu hình Azure rồi thử lại.");
        }
    }
}
