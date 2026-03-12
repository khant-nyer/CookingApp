package com.chef.william.service.auth;

import com.chef.william.config.security.CognitoProperties;
import com.chef.william.dto.auth.RegisterUserRequest;
import com.chef.william.dto.auth.RegisterUserResponse;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.auth.CognitoRegistrationException;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
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
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final UserRepository userRepository;

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
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
                    .userAttributes(AttributeType.builder()
                            .name("email")
                            .value(request.getEmail())
                            .build());

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
            throw new IllegalArgumentException(ex.getMessage());
        } catch (NotAuthorizedException ex) {
            throw new IllegalArgumentException(buildAuthorizationFailureMessage(ex));
        } catch (CognitoIdentityProviderException ex) {
            String detail = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage();
            throw new CognitoRegistrationException("Failed to register user with Cognito: " + detail, ex);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUserName(request.getUserName());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setCognitoSub(signUpResponse.userSub());

        User saved = userRepository.save(user);

        return RegisterUserResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .userName(saved.getUserName())
                .profileImageUrl(saved.getProfileImageUrl())
                .cognitoSub(saved.getCognitoSub())
                .status(signUpResponse.userConfirmed() ? "CONFIRMED" : "PENDING_EMAIL_VERIFICATION")
                .build();
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
