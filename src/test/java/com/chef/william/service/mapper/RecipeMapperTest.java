package com.chef.william.service.mapper;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.model.Recipe;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeMapperTest {

    private final RecipeMapper recipeMapper = new RecipeMapper();

    @Test
    void toDtoShouldUseAuditFieldsFromEntity() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setVersion("v1");
        recipe.setDescription("Sample");
        recipe.setCreatedBy("creator");
        recipe.setUpdatedBy("editor");
        recipe.setUpdatedAt(LocalDateTime.of(2026, 4, 19, 14, 25));
        recipe.setRecipeIngredients(new ArrayList<>());
        recipe.setInstructions(new ArrayList<>());

        RecipeDTO dto = recipeMapper.toDto(recipe);

        assertEquals("creator", dto.getCreatedBy());
        assertEquals("editor", dto.getUpdatedBy());
        assertEquals(LocalDateTime.of(2026, 4, 19, 14, 25), dto.getUpdatedAt());
    }
}
