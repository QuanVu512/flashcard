package com.flashcardapp.repository;

import com.flashcardapp.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID> {

    @Query("""
            select registration from PendingRegistration registration
            where registration.email = :email
              and registration.completedAt is null
              and registration.expiresAt > :now
            order by registration.id
            """)
    List<PendingRegistration> findActiveByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from PendingRegistration registration
            where registration.expiresAt <= :cutoff
               or registration.completedAt <= :cutoff
            """)
    int deleteStale(@Param("cutoff") LocalDateTime cutoff);
}
