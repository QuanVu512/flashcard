package com.flashcardapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "auth_otp_browser_blocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subject_key_hash", "client_key_hash"})
)
public class OtpBrowserBlock {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String subjectKeyHash;

    @Column(nullable = false, length = 64)
    private String clientKeyHash;

    @Column(nullable = false)
    private LocalDateTime blockedUntil;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSubjectKeyHash() {
        return subjectKeyHash;
    }

    public void setSubjectKeyHash(String subjectKeyHash) {
        this.subjectKeyHash = subjectKeyHash;
    }

    public String getClientKeyHash() {
        return clientKeyHash;
    }

    public void setClientKeyHash(String clientKeyHash) {
        this.clientKeyHash = clientKeyHash;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
