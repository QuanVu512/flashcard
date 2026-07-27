package com.flashcardapp.repository;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.FlashcardSet;
import com.flashcardapp.entity.Folder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, UUID> {

    @EntityGraph(attributePaths = {"cards", "folder"})
    List<FlashcardSet> findByClientOrderByCreatedAtDesc(Client client);

    @EntityGraph(attributePaths = {"cards", "folder"})
    List<FlashcardSet> findByClientAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(Client client, String title);

    @EntityGraph(attributePaths = {"cards", "folder"})
    List<FlashcardSet> findByClientAndFolderOrderByCreatedAtDesc(Client client, Folder folder);

    @EntityGraph(attributePaths = {"cards", "folder"})
    Optional<FlashcardSet> findByIdAndClient(UUID id, Client client);

    long countByClientAndFolder(Client client, Folder folder);
}
