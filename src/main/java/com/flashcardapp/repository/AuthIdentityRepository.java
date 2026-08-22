package com.flashcardapp.repository;

import com.flashcardapp.entity.AuthIdentity;
import com.flashcardapp.entity.AuthIdentityProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {

    @EntityGraph(attributePaths = {"user", "user.client"})
    Optional<AuthIdentity> findByProviderAndIssuerAndSubject(
            AuthIdentityProvider provider,
            String issuer,
            String subject
    );

    boolean existsByProviderAndIssuerAndSubject(
            AuthIdentityProvider provider,
            String issuer,
            String subject
    );

    Optional<AuthIdentity> findByUserIdAndProvider(UUID userId, AuthIdentityProvider provider);
}
