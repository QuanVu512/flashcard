package com.flashcardapp.repository;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByClientOrderByCreatedAtDesc(Client client);

    Optional<Folder> findByIdAndClient(UUID id, Client client);
}
