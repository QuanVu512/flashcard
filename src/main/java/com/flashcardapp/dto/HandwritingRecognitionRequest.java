package com.flashcardapp.dto;

public class HandwritingRecognitionRequest {

    private String imageData;
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
