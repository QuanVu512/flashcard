package com.flashcardapp.dto;

public class CardLine {

    private String term;
    private String definition;
    private String phonetic;
    private String example;

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public boolean hasLearningContent() {
        return hasText(term) || hasText(definition) || hasText(phonetic) || hasText(example);
    }

    public boolean isComplete() {
        return hasText(term) && hasText(definition);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
