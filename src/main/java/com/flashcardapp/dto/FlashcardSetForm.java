package com.flashcardapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlashcardSetForm {

    @NotBlank(message = "Vui lòng nhập tên bộ flashcard")
    @Size(max = 180, message = "Tên bộ flashcard tối đa 180 ký tự")
    private String title;

    @Size(max = 600, message = "Mô tả tối đa 600 ký tự")
    private String description;

    private UUID folderId;

    @Valid
    private List<CardLine> cards = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public List<CardLine> getCards() {
        return cards;
    }

    public void setCards(List<CardLine> cards) {
        this.cards = cards;
    }
}
