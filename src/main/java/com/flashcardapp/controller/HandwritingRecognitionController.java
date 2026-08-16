package com.flashcardapp.controller;

import com.flashcardapp.dto.HandwritingRecognitionRequest;
import com.flashcardapp.dto.HandwritingRecognitionResponse;
import com.flashcardapp.service.HandwritingRecognitionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HandwritingRecognitionController {

    private final HandwritingRecognitionService handwritingRecognitionService;

    public HandwritingRecognitionController(HandwritingRecognitionService handwritingRecognitionService) {
        this.handwritingRecognitionService = handwritingRecognitionService;
    }

    @PostMapping("/api/handwriting/recognize")
    public HandwritingRecognitionResponse recognize(@RequestBody HandwritingRecognitionRequest request) {
        try {
            return handwritingRecognitionService.recognize(request.getImageData(), request.getLanguage());
        } catch (Exception ignored) {
            return HandwritingRecognitionResponse.disabled("Nhận dạng chữ viết đang gặp lỗi. Kiểm tra cấu hình Azure Vision rồi thử lại.");
        }
    }
}
