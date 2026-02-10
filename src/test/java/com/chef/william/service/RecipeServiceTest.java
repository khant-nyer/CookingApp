package com.chef.william.service;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.mapper.RecipeMapper;
import com.chef.william.service.recipe.RecipeMergeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private RecipeMergeService recipeMergeService;

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void createRecipeThrowsWhenFoodIdNotFound() {
        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v1");
        dto.setFoodId(404L);
        dto.setIngredients(List.of());
        dto.setInstructions(List.of());

        when(foodRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipeAssignsFoodWhenFoodIdProvided() {
        Food food = new Food();
        food.setId(7L);
        food.setName("Pad Kra Pao");

        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v1");
        dto.setDescription("Classic");
        dto.setFoodId(7L);

        when(foodRepository.findById(7L)).thenReturn(Optional.of(food));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeMapper.toDto(any(Recipe.class))).thenReturn(new RecipeDTO());

        recipeService.createRecipe(dto);

        verify(recipeMergeService).mergeIngredients(any(Recipe.class), any(RecipeDTO.class));
        verify(recipeMergeService).mergeInstructions(any(Recipe.class), any(RecipeDTO.class));
    }
}
