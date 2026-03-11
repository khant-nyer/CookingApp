package com.chef.william.dto.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterUserResponse {
    Long id;
    String email;
    String userName;
    String profileImageUrl;
    String cognitoSub;
    String status;
}
