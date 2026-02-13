package com.chef.william.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodDTO {
    private Long id;

    @NotBlank(message = "Food name is required")
    @Size(max = 180, message = "Food name must not exceed 180 characters")
    private String name;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
    private String imageUrl;

    private Integer recipeCount;

    @Valid
    private List<RecipeDTO> recipes = new ArrayList<>();
}
