package com.flashcardapp.service;

import com.flashcardapp.helper.security.SecureTokenService;
import com.flashcardapp.repository.RegistrationEmailLockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationEmailLockService {

    private static final String KEY_PREFIX = "registration:";

    private final RegistrationEmailLockRepository lockRepository;
    private final RegistrationEmailLockInitializer lockInitializer;
    private final SecureTokenService secureTokenService;

    public RegistrationEmailLockService(RegistrationEmailLockRepository lockRepository,
                                        RegistrationEmailLockInitializer lockInitializer,
                                        SecureTokenService secureTokenService) {
        this.lockRepository = lockRepository;
        this.lockInitializer = lockInitializer;
        this.secureTokenService = secureTokenService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(String normalizedEmail, LocalDateTime now) {
        String emailKeyHash = secureTokenService.hash(KEY_PREFIX + normalizedEmail);
        if (lockRepository.findForUpdate(emailKeyHash).isPresent()) {
            return;
        }

        initialize(emailKeyHash, now);
        lockRepository.findForUpdate(emailKeyHash)
                .orElseThrow(() -> new IllegalStateException(
                        "Không thể khởi tạo khóa đăng ký theo email"
                ));
    }

    private void initialize(String emailKeyHash, LocalDateTime now) {
        try {
            lockInitializer.createIfMissing(emailKeyHash, now);
        } catch (DataIntegrityViolationException ignored) {
            // Một transaction khác đã tạo cùng khóa; truy vấn tiếp theo sẽ chờ khóa đó.
        }
    }
}
