package com.chef.william.dto;

import com.chef.william.model.enums.Unit;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientDTO {
    private Long id;

    @NotNull(message = "Ingredient ID is required")
    private Long ingredientId;

    private String ingredientName;

    @Positive(message = "Quantity must be positive")
    private double quantity;

    @NotNull(message = "Unit is required")
    private Unit unit;

    @Size(max = 200, message = "Note must not exceed 200 characters")
    private String note;
}