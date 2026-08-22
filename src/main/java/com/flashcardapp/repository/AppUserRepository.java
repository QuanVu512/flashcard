package com.flashcardapp.repository;

import com.flashcardapp.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "client")
    Optional<AppUser> findWithClientByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "client")
    Optional<AppUser> findWithClientById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appUser from AppUser appUser where appUser.id = :id")
    Optional<AppUser> findForUpdateById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "client")
    List<AppUser> findAllByOrderByCreatedAtDesc();

    long countByRole(String role);

    boolean existsByEmailIgnoreCase(String email);
}
