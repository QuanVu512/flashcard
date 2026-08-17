package com.flashcardapp.repository;

import com.flashcardapp.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query("select coalesce(sum(client.score), 0) from Client client")
    long sumScore();
}
