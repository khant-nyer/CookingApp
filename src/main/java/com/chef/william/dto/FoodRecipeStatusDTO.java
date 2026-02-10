package com.chef.william.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecipeStatusDTO {
    private Long foodId;
    private String foodName;
    private boolean hasRecipe;
    private String message;
}
