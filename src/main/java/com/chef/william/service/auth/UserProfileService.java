package com.chef.william.service.auth;

import com.chef.william.dto.auth.UpdateProfileRequest;
import com.chef.william.dto.auth.UserProfileResponse;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User currentUser = currentUserService.getRequiredCurrentUser();
        return toResponse(currentUser);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        currentUser.setUserName(request.getUserName());
        currentUser.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getAllergies() != null) {
            currentUser.setAllergies(request.getAllergies());
        }
        User saved = userRepository.save(currentUser);
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .cognitoSub(user.getCognitoSub())
                .email(user.getEmail())
                .userName(user.getUserName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .allergies(user.getAllergies())
                .build();
    }
}
