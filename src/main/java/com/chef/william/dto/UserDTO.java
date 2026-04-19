package com.chef.william.dto;

import com.chef.william.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String userName;
    private String email;

    private String profileImageUrl;
    private UserRole role;
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    private String city;
    // Add other user fields as needed
}
