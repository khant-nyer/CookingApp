package com.chef.william.service.auth;

import com.chef.william.repository.RegistrationIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationIdempotencyCleanupService {

    private final RegistrationIdempotencyRepository idempotencyRepository;

    @Transactional
    @Scheduled(fixedDelayString = "${app.idempotency.registration.cleanup-interval-ms}")
    public void cleanupExpiredRecords() {
        long deleted = idempotencyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired registration idempotency records", deleted);
        }
    }
}
