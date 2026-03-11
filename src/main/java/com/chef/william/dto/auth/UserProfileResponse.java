package com.chef.william.dto.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserProfileResponse {
    Long id;
    String cognitoSub;
    String email;
    String userName;
    String profileImageUrl;
}
