package com.chef.william.service;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.repository.FoodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private FoodService foodService;

    @Test
    void getFoodRecipeStatusShouldShowNoRecipeMessage() {
        Food food = new Food();
        food.setId(1L);
        food.setName("Tom Yum");
        food.setRecipes(new ArrayList<>());

        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));

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
        food.setRecipes(List.of(new Recipe()));

        when(foodRepository.save(any(Food.class))).thenReturn(food);

        FoodDTO result = foodService.createFood(new FoodDTO(null, "Pad Thai", "Noodle", null));

        assertEquals(10L, result.getId());
        assertEquals(1, result.getRecipeCount());
    }
}
