package com.flashcardapp.repository;

import com.flashcardapp.entity.OtpChallenge;
import com.flashcardapp.entity.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId,
            OtpPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpChallenge> findFirstByUserIdAndPurposeAndClientKeyHashAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId,
            OtpPurpose purpose,
            String clientKeyHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge from OtpChallenge challenge
            join fetch challenge.user appUser
            join fetch appUser.client
            where challenge.id = :id
            """)
    Optional<OtpChallenge> findForUpdate(@Param("id") UUID id);
}
