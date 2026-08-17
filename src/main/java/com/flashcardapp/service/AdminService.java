package com.flashcardapp.service;

import com.flashcardapp.dto.admin.AdminStats;
import com.flashcardapp.dto.admin.AdminUserDetail;
import com.flashcardapp.dto.admin.AdminUserRow;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.Client;
import com.flashcardapp.helper.exception.ResourceNotFoundException;
import com.flashcardapp.repository.AppUserRepository;
import com.flashcardapp.repository.ClientRepository;
import com.flashcardapp.repository.FlashcardRepository;
import com.flashcardapp.repository.FlashcardSetRepository;
import com.flashcardapp.repository.FolderRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final ClientRepository clientRepository;
    private final FolderRepository folderRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;

    public AdminService(AppUserRepository appUserRepository,
                        ClientRepository clientRepository,
                        FolderRepository folderRepository,
                        FlashcardSetRepository flashcardSetRepository,
                        FlashcardRepository flashcardRepository) {
        this.appUserRepository = appUserRepository;
        this.clientRepository = clientRepository;
        this.folderRepository = folderRepository;
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        return new AdminStats(
                appUserRepository.count(),
                folderRepository.count(),
                flashcardSetRepository.count(),
                flashcardRepository.count(),
                clientRepository.sumScore()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserRow> users() {
        return appUserRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toUserRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetail userDetail(UUID userId) {
        AppUser user = requireUser(userId);
        Client client = user.getClient();
        return new AdminUserDetail(
                user.getId(),
                user.getEmail(),
                client.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                client.getScore(),
                folderRepository.countByClient(client),
                flashcardSetRepository.countByClient(client),
                flashcardRepository.countByClient(client)
        );
    }

    @Transactional
    public void setEnabled(UUID userId, boolean enabled, String currentAdminEmail) {
        AppUser user = requireUser(userId);
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new AccessDeniedException("Admin không thể tự khóa tài khoản đang đăng nhập.");
        }
        user.setEnabled(enabled);
        appUserRepository.save(user);
    }

    private AppUser requireUser(UUID userId) {
        return appUserRepository.findWithClientById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private AdminUserRow toUserRow(AppUser user) {
        Client client = user.getClient();
        return new AdminUserRow(
                user.getId(),
                user.getEmail(),
                client.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                client.getScore()
        );
    }
}
