package com.chef.william.service.auth;

import com.chef.william.config.security.CognitoProperties;
import com.chef.william.dto.auth.RegisterUserRequest;
import com.chef.william.dto.auth.RegisterUserResponse;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.auth.CognitoRegistrationException;
import com.chef.william.model.User;
import com.chef.william.model.auth.RegistrationIdempotencyRecord;
import com.chef.william.model.auth.RegistrationIdempotencyStatus;
import com.chef.william.repository.RegistrationIdempotencyRepository;
import com.chef.william.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final UserRepository userRepository;
    private final RegistrationIdempotencyRepository idempotencyRepository;

    @Value("${app.idempotency.registration.ttl-minutes:1440}")
    private long idempotencyTtlMinutes;

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("Idempotency-Key header is required for registration requests");
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = calculateRequestHash(request);
        LocalDateTime now = LocalDateTime.now();

        RegistrationIdempotencyRecord record = idempotencyRepository.findByIdempotencyKey(normalizedKey)
                .map(existing -> handleExistingRecord(existing, requestHash, now))
                .orElseGet(() -> createInProgressRecord(normalizedKey, requestHash, now));

        if (record.getStatus() == RegistrationIdempotencyStatus.COMPLETED) {
            return toResponse(record);
        }

        try {
            RegisterUserResponse response = performRegistration(request);
            markCompleted(record, response);
            return response;
        } catch (RuntimeException ex) {
            record.setStatus(RegistrationIdempotencyStatus.FAILED);
            idempotencyRepository.save(record);
            throw ex;
        }
    }

    private RegistrationIdempotencyRecord handleExistingRecord(RegistrationIdempotencyRecord existing,
                                                                String requestHash,
                                                                LocalDateTime now) {
        if (existing.getExpiresAt().isBefore(now)) {
            idempotencyRepository.delete(existing);
            return createInProgressRecord(existing.getIdempotencyKey(), requestHash, now);
        }

        if (!existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException("Idempotency key was already used with a different registration payload");
        }

        if (existing.getStatus() == RegistrationIdempotencyStatus.IN_PROGRESS) {
            throw new BusinessException("Registration request is already being processed for this idempotency key");
        }

        return existing;
    }

    private RegistrationIdempotencyRecord createInProgressRecord(String idempotencyKey,
                                                                  String requestHash,
                                                                  LocalDateTime now) {
        RegistrationIdempotencyRecord record = new RegistrationIdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(RegistrationIdempotencyStatus.IN_PROGRESS);
        record.setExpiresAt(now.plusMinutes(idempotencyTtlMinutes));
        try {
            return idempotencyRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    private RegisterUserResponse performRegistration(RegisterUserRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("User", "email", request.getEmail());
                });

        SignUpResponse signUpResponse;
        try {
            SignUpRequest.Builder signUpBuilder = SignUpRequest.builder()
                    .clientId(cognitoProperties.getAppClientId())
                    .username(request.getEmail())
                    .password(request.getPassword())
                    .userAttributes(
                            AttributeType.builder()
                                    .name("email")
                                    .value(request.getEmail())
                                    .build(),
                            AttributeType.builder()
                                    .name("preferred_username")
                                    .value(request.getUserName())
                                    .build()
                    );

            if (cognitoProperties.hasAppClientSecret()) {
                signUpBuilder.secretHash(calculateSecretHash(
                        request.getEmail(),
                        cognitoProperties.getAppClientId(),
                        cognitoProperties.getAppClientSecret()
                ));
            }

            signUpResponse = cognitoClient.signUp(signUpBuilder.build());
        } catch (UsernameExistsException ex) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        } catch (InvalidPasswordException | InvalidParameterException ex) {
            throw new BusinessException(ex.getMessage());
        } catch (NotAuthorizedException ex) {
            throw new BusinessException(buildAuthorizationFailureMessage(ex));
        } catch (CognitoIdentityProviderException ex) {
            String detail = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage();
            throw new CognitoRegistrationException("Failed to register user with Cognito: " + detail, ex);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUserName(request.getUserName());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setCognitoSub(signUpResponse.userSub());

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (RuntimeException ex) {
            rollbackCognitoRegistration(request.getEmail());
            throw new CognitoRegistrationException(
                    "User was created in Cognito but failed to persist in local database", ex);
        }

        return RegisterUserResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .userName(saved.getUserName())
                .profileImageUrl(saved.getProfileImageUrl())
                .cognitoSub(saved.getCognitoSub())
                .status(signUpResponse.userConfirmed() ? "CONFIRMED" : "PENDING_EMAIL_VERIFICATION")
                .build();
    }

    private void markCompleted(RegistrationIdempotencyRecord record, RegisterUserResponse response) {
        record.setStatus(RegistrationIdempotencyStatus.COMPLETED);
        record.setResponseUserId(response.getId());
        record.setResponseEmail(response.getEmail());
        record.setResponseUserName(response.getUserName());
        record.setResponseProfileImageUrl(response.getProfileImageUrl());
        record.setResponseCognitoSub(response.getCognitoSub());
        record.setResponseStatus(response.getStatus());
        idempotencyRepository.save(record);
    }

    private RegisterUserResponse toResponse(RegistrationIdempotencyRecord record) {
        return RegisterUserResponse.builder()
                .id(record.getResponseUserId())
                .email(record.getResponseEmail())
                .userName(record.getResponseUserName())
                .profileImageUrl(record.getResponseProfileImageUrl())
                .cognitoSub(record.getResponseCognitoSub())
                .status(record.getResponseStatus())
                .build();
    }

    private void rollbackCognitoRegistration(String email) {
        try {
            cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .username(email)
                    .build());
            log.warn("Rolled back Cognito user after local persistence failure for email={}", email);
        } catch (CognitoIdentityProviderException rollbackEx) {
            log.error("Failed to rollback Cognito user for email={}", email, rollbackEx);
        }
    }

    private String calculateRequestHash(RegisterUserRequest request) {
        String raw = request.getEmail() + "|" + request.getUserName() + "|" + request.getPassword() + "|"
                + (request.getProfileImageUrl() == null ? "" : request.getProfileImageUrl());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate request hash", ex);
        }
    }

    private String calculateSecretHash(String username, String clientId, String clientSecret) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            hmacSha256.update(username.getBytes(StandardCharsets.UTF_8));
            byte[] raw = hmacSha256.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate Cognito secret hash", ex);
        }
    }

    private String buildAuthorizationFailureMessage(NotAuthorizedException ex) {
        String detail = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage();

        if (cognitoProperties.hasAppClientSecret()) {
            return "Cognito authorization failed during sign-up: " + detail +
                    ". Check security.cognito.app-client-secret and app client settings in Cognito.";
        }

        return "Cognito authorization failed during sign-up: " + detail +
                ". If your app client has a secret, set security.cognito.app-client-secret in backend config.";
    }
}
