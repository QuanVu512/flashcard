package com.flashcardapp.repository;

import com.flashcardapp.entity.OtpBrowserBlock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpBrowserBlockRepository extends JpaRepository<OtpBrowserBlock, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select block from OtpBrowserBlock block
            where block.subjectKeyHash = :subjectKeyHash and block.clientKeyHash = :clientKeyHash
            """)
    Optional<OtpBrowserBlock> findForUpdate(
            @Param("subjectKeyHash") String subjectKeyHash,
            @Param("clientKeyHash") String clientKeyHash
    );

    long deleteByBlockedUntilLessThanEqual(java.time.LocalDateTime blockedUntil);
}
