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
            where block.userId = :userId and block.clientKeyHash = :clientKeyHash
            """)
    Optional<OtpBrowserBlock> findForUpdate(
            @Param("userId") UUID userId,
            @Param("clientKeyHash") String clientKeyHash
    );
}
