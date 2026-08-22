package com.flashcardapp.repository;

import com.flashcardapp.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from RefreshSession session
            join fetch session.user appUser
            join fetch appUser.client
            where session.id = :id
            """)
    Optional<RefreshSession> findForUpdate(@Param("id") UUID id);
}
