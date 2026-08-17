package com.flashcardapp.repository;

import com.flashcardapp.entity.Client;
import com.flashcardapp.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    @Query("select count(card) from Flashcard card where card.flashcardSet.client = :client")
    long countByClient(@Param("client") Client client);
}
