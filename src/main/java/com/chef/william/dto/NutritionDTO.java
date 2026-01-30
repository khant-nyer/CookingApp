package com.chef.william.dto;

import com.chef.william.model.enums.Nutrients;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionDTO {
    private Long id;

    @NotNull(message = "Nutrient type is required")
    private Nutrients nutrient;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double value;

    @NotBlank(message = "Unit is required")
    private String unit;


}