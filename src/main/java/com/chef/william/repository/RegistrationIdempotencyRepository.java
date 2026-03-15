package com.chef.william.repository;

import com.chef.william.model.auth.RegistrationIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RegistrationIdempotencyRepository extends JpaRepository<RegistrationIdempotencyRecord, Long> {
    Optional<RegistrationIdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    long deleteByExpiresAtBefore(LocalDateTime threshold);
}
