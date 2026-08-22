package com.flashcardapp.service;

import com.flashcardapp.entity.RegistrationEmailLock;
import com.flashcardapp.repository.RegistrationEmailLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationEmailLockInitializer {

    private final RegistrationEmailLockRepository lockRepository;

    public RegistrationEmailLockInitializer(RegistrationEmailLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfMissing(String emailKeyHash, LocalDateTime createdAt) {
        if (lockRepository.existsById(emailKeyHash)) {
            return;
        }

        RegistrationEmailLock emailLock = new RegistrationEmailLock();
        emailLock.setEmailKeyHash(emailKeyHash);
        emailLock.setCreatedAt(createdAt);
        lockRepository.saveAndFlush(emailLock);
    }
}
