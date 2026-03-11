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
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

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
            signUpResponse = cognitoClient.signUp(SignUpRequest.builder()
                    .clientId(cognitoProperties.getAppClientId())
                    .username(request.getEmail())
                    .password(request.getPassword())
                    .userAttributes(
                            software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType.builder()
                                    .name("email")
                                    .value(request.getEmail())
                                    .build()
                    )
                    .build());
        } catch (UsernameExistsException ex) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        } catch (InvalidPasswordException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        } catch (CognitoIdentityProviderException ex) {
            throw new CognitoRegistrationException("Failed to register user with Cognito", ex);
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
}
