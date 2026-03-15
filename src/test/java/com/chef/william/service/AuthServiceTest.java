package com.chef.william.service;

import com.chef.william.config.security.CognitoProperties;
import com.chef.william.dto.auth.RegisterUserRequest;
import com.chef.william.dto.auth.RegisterUserResponse;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.auth.CognitoRegistrationException;
import com.chef.william.model.auth.RegistrationIdempotencyRecord;
import com.chef.william.model.auth.RegistrationIdempotencyStatus;
import com.chef.william.repository.RegistrationIdempotencyRepository;
import com.chef.william.repository.UserRepository;
import com.chef.william.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InternalErrorException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InternalErrorException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationIdempotencyRepository idempotencyRepository;

    private CognitoProperties properties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        properties = new CognitoProperties();
        properties.setAppClientId("app-client-id");
        properties.setRegion("ap-southeast-2");
        properties.setUserPoolId("pool-id");

        authService = new AuthService(cognitoClient, properties, userRepository, idempotencyRepository);
    }

    @Test
    void registerShouldCreateUserAndReturnPendingVerificationStatus() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");
        request.setProfileImageUrl("https://example.com/me.jpg");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.chef.william.model.User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        RegisterUserResponse response = authService.register(request, "idem-1");

        assertEquals(10L, response.getId());
        assertEquals("sub-123", response.getCognitoSub());
        assertEquals("PENDING_EMAIL_VERIFICATION", response.getStatus());

        ArgumentCaptor<com.chef.william.model.User> captor = ArgumentCaptor.forClass(com.chef.william.model.User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("https://example.com/me.jpg", captor.getValue().getProfileImageUrl());

        ArgumentCaptor<SignUpRequest> signUpCaptor = ArgumentCaptor.forClass(SignUpRequest.class);
        verify(cognitoClient).signUp(signUpCaptor.capture());
        Map<String, String> attributesByName = signUpCaptor.getValue().userAttributes().stream()
                .collect(Collectors.toMap(attribute -> attribute.name(), attribute -> attribute.value()));
        assertEquals("chef@example.com", attributesByName.get("email"));
        assertEquals("chef", attributesByName.get("preferred_username"));
    }

    @Test
    void registerShouldThrowWhenIdempotencyKeyMissing() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request, "  "));
        assertEquals("Idempotency-Key header is required for registration requests", ex.getMessage());
    }

    @Test
    void registerShouldReturnStoredResponseForCompletedKey() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        RegistrationIdempotencyRecord record = new RegistrationIdempotencyRecord();
        record.setIdempotencyKey("idem-1");
        record.setRequestHash("fec1c9277104c4de50798625f474564bf381f6a1714b54e29c695569b20f3cb3");
        record.setStatus(RegistrationIdempotencyStatus.COMPLETED);
        record.setResponseUserId(55L);
        record.setResponseEmail("chef@example.com");
        record.setResponseUserName("chef");
        record.setResponseCognitoSub("sub-x");
        record.setResponseStatus("PENDING_EMAIL_VERIFICATION");
        record.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(record));

        RegisterUserResponse response = authService.register(request, "idem-1");

        assertEquals(55L, response.getId());
        assertEquals("sub-x", response.getCognitoSub());
        verify(cognitoClient, never()).signUp(any(SignUpRequest.class));
    }


    @Test
    void registerShouldThrowWhenSameKeyUsedWithDifferentPayload() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        RegistrationIdempotencyRecord record = new RegistrationIdempotencyRecord();
        record.setIdempotencyKey("idem-1");
        record.setRequestHash("different-hash");
        record.setStatus(RegistrationIdempotencyStatus.IN_PROGRESS);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request, "idem-1"));
        assertEquals("Idempotency key was already used with a different registration payload", ex.getMessage());
    }

    @Test
    void registerShouldIncludeSecretHashWhenClientSecretConfigured() {
        properties.setAppClientSecret("super-secret");

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request, "idem-1");

        ArgumentCaptor<SignUpRequest> requestCaptor = ArgumentCaptor.forClass(SignUpRequest.class);
        verify(cognitoClient).signUp(requestCaptor.capture());
        assertNotNull(requestCaptor.getValue().secretHash());
    }


    @Test
    void registerShouldGiveSecretHintWhenNotAuthorizedAndSecretNotConfigured() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenThrow(NotAuthorizedException.builder()
                .message("Client secret mismatch")
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request, "idem-1"));

        assertEquals(
                "Cognito authorization failed during sign-up: Client secret mismatch. If your app client has a secret, set security.cognito.app-client-secret in backend config.",
                exception.getMessage()
        );
    }

    @Test
    void registerShouldSuggestValidatingConfiguredSecretWhenNotAuthorized() {
        properties.setAppClientSecret("super-secret");

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenThrow(NotAuthorizedException.builder()
                .message("Unable to verify secret hash for client")
                .build());

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request, "idem-1"));

        assertEquals(
                "Cognito authorization failed during sign-up: Unable to verify secret hash for client. Check security.cognito.app-client-secret and app client settings in Cognito.",
                exception.getMessage()
        );
    }

    @Test
    void registerShouldThrowConflictWhenEmailExistsInDb() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(new com.chef.william.model.User()));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request, "idem-1"));
    }

    @Test
    void registerShouldRollbackCognitoUserWhenDatabaseSaveFails() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThrows(CognitoRegistrationException.class, () -> authService.register(request, "idem-1"));

        ArgumentCaptor<AdminDeleteUserRequest> deleteCaptor = ArgumentCaptor.forClass(AdminDeleteUserRequest.class);
        verify(cognitoClient).adminDeleteUser(deleteCaptor.capture());
        assertEquals("pool-id", deleteCaptor.getValue().userPoolId());
        assertEquals("chef@example.com", deleteCaptor.getValue().username());
    }

    @Test
    void registerShouldStillThrowWhenRollbackFails() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenThrow(new RuntimeException("db down"));
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenThrow(InternalErrorException.builder().message("rollback failed").build());

        assertThrows(CognitoRegistrationException.class, () -> authService.register(request, "idem-1"));
        verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
    }

    @Test
    void registerShouldNotCallCognitoWhenEmailAlreadyExistsInDb() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(idempotencyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(idempotencyRepository.save(any(RegistrationIdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(new com.chef.william.model.User()));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request, "idem-1"));
        verify(cognitoClient, never()).signUp(any(SignUpRequest.class));
    }

    @Test
    void registerShouldRollbackCognitoUserWhenDatabaseSaveFails() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThrows(CognitoRegistrationException.class, () -> authService.register(request));

        ArgumentCaptor<AdminDeleteUserRequest> deleteCaptor = ArgumentCaptor.forClass(AdminDeleteUserRequest.class);
        verify(cognitoClient).adminDeleteUser(deleteCaptor.capture());
        assertEquals("pool-id", deleteCaptor.getValue().userPoolId());
        assertEquals("chef@example.com", deleteCaptor.getValue().username());
    }

    @Test
    void registerShouldStillThrowWhenRollbackFails() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenThrow(new RuntimeException("db down"));
        when(cognitoClient.adminDeleteUser(any(AdminDeleteUserRequest.class)))
                .thenThrow(InternalErrorException.builder().message("rollback failed").build());

        assertThrows(CognitoRegistrationException.class, () -> authService.register(request));
        verify(cognitoClient).adminDeleteUser(any(AdminDeleteUserRequest.class));
    }

    @Test
    void registerShouldNotCallCognitoWhenEmailAlreadyExistsInDb() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(new com.chef.william.model.User()));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(cognitoClient, never()).signUp(any(SignUpRequest.class));
    }

}
