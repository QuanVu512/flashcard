package com.flashcardapp.repository;

import com.flashcardapp.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {
}
