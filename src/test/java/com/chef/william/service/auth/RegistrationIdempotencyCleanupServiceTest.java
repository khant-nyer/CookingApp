package com.chef.william.service.auth;

import com.chef.william.repository.RegistrationIdempotencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationIdempotencyCleanupServiceTest {

    @Mock
    private RegistrationIdempotencyRepository idempotencyRepository;

    @InjectMocks
    private RegistrationIdempotencyCleanupService cleanupService;

    @Test
    void cleanupExpiredRecordsShouldDeleteUsingCurrentThreshold() {
        when(idempotencyRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(2L);

        cleanupService.cleanupExpiredRecords();

        verify(idempotencyRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
