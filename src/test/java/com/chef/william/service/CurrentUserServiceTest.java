package com.chef.william.service;

import com.chef.william.exception.UnauthorizedException;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import com.chef.william.service.auth.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveCurrentUserFromJwtSub() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"), Map.of("sub", "sub-123"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        User user = new User();
        user.setId(5L);
        user.setCognitoSub("sub-123");
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(user));

        CurrentUserService service = new CurrentUserService(userRepository);
        assertEquals(5L, service.getRequiredCurrentUser().getId());
    }


    @Test
    void shouldSyncEmailVerificationFromJwtAndActivatePendingUser() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"), Map.of("sub", "sub-123", "email_verified", true));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        User user = new User();
        user.setId(5L);
        user.setCognitoSub("sub-123");
        user.setEmailVerified(false);
        user.setAccountStatus(com.chef.william.model.AccountStatus.PENDING_EMAIL_VERIFICATION);
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        CurrentUserService service = new CurrentUserService(userRepository);
        User resolved = service.getRequiredCurrentUser();

        assertEquals(5L, resolved.getId());
        assertTrue(resolved.isEmailVerified());
        assertEquals(com.chef.william.model.AccountStatus.ACTIVE, resolved.getAccountStatus());
        verify(userRepository).save(user);
    }

    @Test
    void shouldNotPersistWhenVerificationStateUnchanged() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"), Map.of("sub", "sub-123", "email_verified", false));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        User user = new User();
        user.setId(5L);
        user.setCognitoSub("sub-123");
        user.setEmailVerified(false);
        user.setAccountStatus(com.chef.william.model.AccountStatus.PENDING_EMAIL_VERIFICATION);
        when(userRepository.findByCognitoSub("sub-123")).thenReturn(Optional.of(user));

        CurrentUserService service = new CurrentUserService(userRepository);
        service.getRequiredCurrentUser();

        verify(userRepository, never()).save(user);
    }
    @Test
    void shouldThrowUnauthorizedWhenTokenMissing() {
        CurrentUserService service = new CurrentUserService(userRepository);
        assertThrows(UnauthorizedException.class, service::getRequiredCurrentUser);
    }
}
