package com.flashcardapp.service;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import com.flashcardapp.helper.exception.ResourceNotFoundException;
import com.flashcardapp.repository.FlashcardSetRepository;
import com.flashcardapp.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final FlashcardSetRepository flashcardSetRepository;

    public FolderService(FolderRepository folderRepository,
                         FlashcardSetRepository flashcardSetRepository) {
        this.folderRepository = folderRepository;
        this.flashcardSetRepository = flashcardSetRepository;
    }

    @Transactional(readOnly = true)
    public List<Folder> foldersFor(Client client) {
        return folderRepository.findByClientOrderByCreatedAtDesc(client);
    }

    @Transactional
    public Folder create(Client client, String name, String description) {
        Folder folder = new Folder();
        folder.setClient(client);
        updateFields(folder, name, description);
        return folderRepository.save(folder);
    }

    @Transactional
    public Folder update(Client client, UUID folderId, String name, String description) {
        Folder folder = requireOwnedFolder(client, folderId);
        updateFields(folder, name, description);
        return folderRepository.save(folder);
    }

    @Transactional
    public void delete(Client client, UUID folderId) {
        Folder folder = requireOwnedFolder(client, folderId);
        List<FlashcardSet> folderSets = flashcardSetRepository
                .findByClientAndFolderOrderByCreatedAtDesc(client, folder);
        folderSets.forEach(set -> set.setFolder(null));
        flashcardSetRepository.saveAll(folderSets);
        flashcardSetRepository.flush();
        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Folder requireOwnedFolder(Client client, UUID folderId) {
        return folderRepository.findByIdAndClient(folderId, client)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thư mục"));
    }

    private void updateFields(Folder folder, String name, String description) {
        folder.setName(name.trim());
        folder.setDescription(trimToNull(description));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
