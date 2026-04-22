package com.chef.william.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    @NotBlank
    private String userName;

    private String profileImageUrl;

    private List<String> allergies;
}
