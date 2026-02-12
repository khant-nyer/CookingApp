package com.chef.william.service;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.mapper.RecipeMapper;
import com.chef.william.model.enums.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private RecipeService recipeService;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private FoodService foodService;

    @Test
    void getFoodRecipeStatusShouldShowNoRecipeMessage() {
        Food food = new Food();
        food.setId(1L);
        food.setName("Tom Yum");
        food.setRecipes(new ArrayList<>());

        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        when(recipeRepository.countByFoodId(1L)).thenReturn(0);

        FoodRecipeStatusDTO status = foodService.getFoodRecipeStatus(1L);

        assertEquals(false, status.isHasRecipe());
        assertEquals("There is no recipe for this food yet, create one", status.getMessage());
    }

    @Test
    void createFoodShouldReturnMappedDto() {
        Food food = new Food();
        food.setId(10L);
        food.setName("Pad Thai");
        food.setCategory("Noodle");
        Recipe recipe = new Recipe();
        recipe.setId(2L);
        recipe.setVersion("v1");
        food.setRecipes(List.of(recipe));

        RecipeDTO recipeDTO = RecipeDTO.builder().id(2L).version("v1").build();

        when(foodRepository.save(any(Food.class))).thenReturn(food);
        when(recipeRepository.countByFoodId(10L)).thenReturn(1);
        when(recipeMapper.toDto(recipe)).thenReturn(recipeDTO);

        FoodDTO result = foodService.createFood(new FoodDTO(null, "Pad Thai", "Noodle", null, List.of()));

        assertEquals(10L, result.getId());
        assertEquals(1, result.getRecipeCount());
        assertEquals(1, result.getRecipes().size());
        assertEquals(2L, result.getRecipes().get(0).getId());
    }

    @Test
    void createFoodShouldCreateRecipeVersionsWhenProvided() {
        Food food = new Food();
        food.setId(12L);
        food.setName("Som Tum");
        food.setCategory("Salad");

        RecipeDTO recipeDTO = RecipeDTO.builder()
                .version("v1")
                .description("Spicy")
                .ingredients(List.of(new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null)))
                .instructions(List.of(InstructionDTO.builder().step(1).description("Mix").build()))
                .build();

        when(foodRepository.save(any(Food.class))).thenReturn(food);
        when(recipeRepository.countByFoodId(12L)).thenReturn(1);

        foodService.createFood(new FoodDTO(null, "Som Tum", "Salad", null, List.of(recipeDTO)));

        verify(recipeService).createRecipe(any(RecipeDTO.class));
    }

    @Test
    void createFoodShouldThrowWhenNameAlreadyExists() {
        when(foodRepository.existsByNameIgnoreCase("Ramen")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> foodService.createFood(new FoodDTO(null, "Ramen", "Noodle", null, List.of())));

        verify(foodRepository, never()).save(any(Food.class));
    }
}
