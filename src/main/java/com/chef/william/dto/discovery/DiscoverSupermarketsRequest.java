package com.chef.william.dto.discovery;

import jakarta.validation.constraints.NotBlank;

public record DiscoverSupermarketsRequest(
        @NotBlank(message = "ingredient is required") String ingredient,
        @NotBlank(message = "city is required") String city
) {
}
