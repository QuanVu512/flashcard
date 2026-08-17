package com.flashcardapp.dto;

import jakarta.validation.constraints.Size;

public class HandwritingRecognitionRequest {

    @Size(max = 3_000_000, message = "Ảnh vẽ quá lớn")
    private String imageData;

    @Size(max = 16, message = "Mã ngôn ngữ không hợp lệ")
    private String language;

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
