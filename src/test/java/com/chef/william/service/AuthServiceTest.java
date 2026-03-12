package com.chef.william.service;

import com.chef.william.config.security.CognitoProperties;
import com.chef.william.dto.auth.RegisterUserRequest;
import com.chef.william.dto.auth.RegisterUserResponse;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.repository.UserRepository;
import com.chef.william.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @Mock
    private UserRepository userRepository;

    private CognitoProperties properties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        properties = new CognitoProperties();
        properties.setAppClientId("app-client-id");
        properties.setRegion("ap-southeast-2");
        properties.setUserPoolId("pool-id");

        authService = new AuthService(cognitoClient, properties, userRepository);
    }

    @Test
    void registerShouldCreateUserAndReturnPendingVerificationStatus() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");
        request.setProfileImageUrl("https://example.com/me.jpg");

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any())).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            com.chef.william.model.User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        RegisterUserResponse response = authService.register(request);

        assertEquals(10L, response.getId());
        assertEquals("sub-123", response.getCognitoSub());
        assertEquals("PENDING_EMAIL_VERIFICATION", response.getStatus());

        ArgumentCaptor<com.chef.william.model.User> captor = ArgumentCaptor.forClass(com.chef.william.model.User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("https://example.com/me.jpg", captor.getValue().getProfileImageUrl());
    }

    @Test
    void registerShouldIncludeSecretHashWhenClientSecretConfigured() {
        properties.setAppClientSecret("super-secret");

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("chef@example.com");
        request.setUserName("chef");
        request.setPassword("MyPassword123!");

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any())).thenReturn(SignUpResponse.builder()
                .userSub("sub-123")
                .userConfirmed(false)
                .build());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

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

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any())).thenThrow(NotAuthorizedException.builder()
                .message("Client secret mismatch")
                .build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

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

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.empty());
        when(cognitoClient.signUp(any())).thenThrow(NotAuthorizedException.builder()
                .message("Unable to verify secret hash for client")
                .build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

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

        when(userRepository.findByEmail("chef@example.com")).thenReturn(Optional.of(new com.chef.william.model.User()));

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }
}
