package com.chef.william.dto.auth;

import com.chef.william.model.UserRole;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserProfileResponse {
    Long id;
    String cognitoSub;
    String email;
    String userName;
    String profileImageUrl;
    UserRole role;
    List<String> allergies;
}
