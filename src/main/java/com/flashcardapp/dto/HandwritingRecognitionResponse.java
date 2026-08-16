package com.flashcardapp.dto;

public class HandwritingRecognitionResponse {

    private boolean enabled;
    private String text;
    private String message;

    public static HandwritingRecognitionResponse disabled(String message) {
        HandwritingRecognitionResponse response = new HandwritingRecognitionResponse();
        response.setEnabled(false);
        response.setText("");
        response.setMessage(message);
        return response;
    }

    public static HandwritingRecognitionResponse success(String text, String message) {
        HandwritingRecognitionResponse response = new HandwritingRecognitionResponse();
        response.setEnabled(true);
        response.setText(text == null ? "" : text);
        response.setMessage(message);
        return response;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
