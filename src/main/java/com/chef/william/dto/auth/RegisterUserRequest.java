package com.chef.william.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String userName;

    @NotBlank
    private String password;

    private String profileImageUrl;
}
