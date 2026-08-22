package com.flashcardapp.repository;

import com.flashcardapp.entity.OtpMailDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpMailDeliveryRepository extends JpaRepository<OtpMailDelivery, UUID> {

    long countByQuotaKeyHashAndCreatedAtGreaterThanEqual(String quotaKeyHash, LocalDateTime createdAt);

    Optional<OtpMailDelivery> findFirstByQuotaKeyHashAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            String quotaKeyHash,
            LocalDateTime createdAt
    );

    Optional<OtpMailDelivery> findFirstByChallenge_IdOrderByCreatedAtAsc(UUID challengeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from OtpMailDelivery delivery where delivery.id = :id")
    Optional<OtpMailDelivery> findForUpdate(@Param("id") UUID id);

    @Query(value = """
            select delivery.*
            from auth_otp_mail_deliveries delivery
            where delivery.status in ('PENDING', 'PROCESSING')
              and delivery.available_at <= :now
              and exists (
                  select 1
                  from auth_otp_challenges challenge
                  where challenge.id = delivery.challenge_id
                    and challenge.consumed_at is null
                    and challenge.expires_at > :now
                    and (
                        challenge.pending_registration_id is null
                        or exists (
                            select 1
                            from auth_pending_registrations pending_registration
                            where pending_registration.id = challenge.pending_registration_id
                              and pending_registration.completed_at is null
                              and pending_registration.expires_at > :now
                        )
                    )
              )
            order by delivery.created_at
            fetch first 1 row only
            for update skip locked
            """, nativeQuery = true)
    Optional<OtpMailDelivery> findReadyForUpdate(@Param("now") LocalDateTime now);
}
