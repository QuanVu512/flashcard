package com.flashcardapp.repository;

import com.flashcardapp.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "client")
    Optional<AppUser> findWithClientById(UUID id);

    @EntityGraph(attributePaths = "client")
    List<AppUser> findAllByOrderByCreatedAtDesc();

    long countByRole(String role);

    boolean existsByEmailIgnoreCase(String email);
}
