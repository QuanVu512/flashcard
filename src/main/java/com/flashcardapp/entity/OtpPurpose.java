package com.flashcardapp.entity;

public enum OtpPurpose {
    REGISTRATION,
    EMAIL_VERIFICATION,
    LOGIN;

    public boolean verifiesEmail() {
        return this == REGISTRATION || this == EMAIL_VERIFICATION;
    }
}
