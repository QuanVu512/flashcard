package com.flashcardapp.repository;

import com.flashcardapp.entity.OtpMailDelivery;
import com.flashcardapp.entity.OtpMailDeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpMailDeliveryRepository extends JpaRepository<OtpMailDelivery, UUID> {

    long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, LocalDateTime createdAt);

    Optional<OtpMailDelivery> findFirstByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            UUID userId,
            LocalDateTime createdAt
    );

    Optional<OtpMailDelivery> findFirstByChallenge_IdOrderByCreatedAtAsc(UUID challengeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from OtpMailDelivery delivery where delivery.id = :id")
    Optional<OtpMailDelivery> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery from OtpMailDelivery delivery
            join fetch delivery.challenge challenge
            where delivery.status in :statuses
              and delivery.availableAt <= :now
              and challenge.consumedAt is null
              and challenge.expiresAt > :now
            order by delivery.createdAt
            """)
    List<OtpMailDelivery> findReady(
            @Param("statuses") Collection<OtpMailDeliveryStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
