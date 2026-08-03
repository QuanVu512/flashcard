package com.flashcardapp.dto;

import java.util.ArrayList;
import java.util.List;

public class TranslationSuggestionResponse {

    private boolean enabled;
    private String message;
    private String inputText;
    private String sourceLanguage;
    private String detectedLanguage;
    private String targetLanguage;
    private String phoneticMessage;
    private List<String> suggestions = new ArrayList<>();
    private List<String> phoneticSuggestions = new ArrayList<>();

    public static TranslationSuggestionResponse disabled(String message) {
        TranslationSuggestionResponse response = new TranslationSuggestionResponse();
        response.setEnabled(false);
        response.setMessage(message);
        return response;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getPhoneticMessage() {
        return phoneticMessage;
    }

    public void setPhoneticMessage(String phoneticMessage) {
        this.phoneticMessage = phoneticMessage;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
    }

    public List<String> getPhoneticSuggestions() {
        return phoneticSuggestions;
    }

    public void setPhoneticSuggestions(List<String> phoneticSuggestions) {
        this.phoneticSuggestions = phoneticSuggestions == null ? new ArrayList<>() : phoneticSuggestions;
    }
}
