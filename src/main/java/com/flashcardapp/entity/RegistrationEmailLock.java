package com.flashcardapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_registration_email_locks")
public class RegistrationEmailLock {

    @Id
    @Column(length = 64)
    private String emailKeyHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public String getEmailKeyHash() {
        return emailKeyHash;
    }

    public void setEmailKeyHash(String emailKeyHash) {
        this.emailKeyHash = emailKeyHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
