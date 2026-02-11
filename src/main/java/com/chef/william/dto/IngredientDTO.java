package com.chef.william.dto;

import com.chef.william.model.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Serving amount is required")
    @Positive(message = "Serving amount must be positive")
    private Double servingAmount = 100.0;

    @NotNull(message = "Serving unit is required")
    private Unit servingUnit = Unit.G;

    private List<NutritionDTO> nutritionList = new ArrayList<>();


    // Cached location-aware store matches for this ingredient.
    // This list is response-oriented and usually refreshed by an external provider.
    private List<IngredientStoreListingDTO> nearbyStoreListings = new ArrayList<>();
}