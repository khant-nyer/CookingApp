package com.chef.william.service.auth;

import com.chef.william.dto.auth.UpdateProfileRequest;
import com.chef.william.dto.auth.UserProfileResponse;
import com.chef.william.model.User;
import com.chef.william.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void updateMyProfileShouldUpdateAllergiesWhenPresent() {
        User user = new User();
        user.setId(5L);
        user.setCognitoSub("sub-123");
        user.setEmail("user@example.com");
        user.setUserName("old-name");
        user.setProfileImageUrl("https://example.com/old.png");
        user.setAllergies(List.of("Peanut"));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUserName("new-name");
        request.setProfileImageUrl("https://example.com/new.png");
        request.setAllergies(List.of("Seafood"));

        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateMyProfile(request);

        assertEquals("new-name", response.getUserName());
        assertEquals("https://example.com/new.png", response.getProfileImageUrl());
        assertEquals(List.of("Seafood"), response.getAllergies());
    }
}
