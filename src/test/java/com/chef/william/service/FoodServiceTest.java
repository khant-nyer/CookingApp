package com.chef.william.service;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.model.User;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.FoodRecipeCountProjection;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.auth.CurrentUserService;
import com.chef.william.service.mapper.RecipeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
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
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private FoodService foodService;

    @Test
    void getFoodRecipeStatusShouldShowNoRecipeMessage() {
        Food food = new Food();
        food.setId(1L);
        food.setName("Tom Yum");
        food.setRecipes(new ArrayList<>());

        when(foodRepository.findById(1L)).thenReturn(Optional.of(food));
        when(recipeRepository.existsByFoodId(1L)).thenReturn(false);

        FoodRecipeStatusDTO status = foodService.getFoodRecipeStatus(1L);

        assertFalse(status.isHasRecipe());
        assertEquals("There is no recipe for this food yet, create one", status.getMessage());
    }

    @Test
    void createFoodShouldReturnMappedDto() {
        User user = new User();
        user.setUserName("tester");
        Food food = new Food();
        food.setId(10L);
        food.setName("Pad Thai");
        food.setCategory("Noodle");
        food.setImageUrl("https://img.example/pad-thai.jpg");
        Recipe recipe = new Recipe();
        recipe.setId(2L);
        recipe.setVersion("v1");

        RecipeDTO recipeDTO = RecipeDTO.builder().id(2L).version("v1").build();

        when(foodRepository.save(any(Food.class))).thenReturn(food);
        when(recipeRepository.findDetailedByFoodId(10L)).thenReturn(List.of(recipe));
        when(recipeMapper.toDto(recipe)).thenReturn(recipeDTO);
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        FoodDTO result = foodService.createFood(new FoodDTO(null, "Pad Thai", "Noodle",
                "https://img.example/pad-thai.jpg", null, null, null, null, List.of()));

        assertEquals(10L, result.getId());
        assertEquals(1, result.getRecipeCount());
        assertEquals(1, result.getRecipes().size());
        assertEquals(2L, result.getRecipes().getFirst().getId());
        assertEquals("https://img.example/pad-thai.jpg", result.getImageUrl());
    }

    @Test
    void createFoodShouldPopulateAuditFields() {
        User user = new User();
        user.setUserName("tester");
        Food food = new Food();
        food.setId(20L);
        food.setName("Khao Pad");
        food.setCategory("Rice");
        food.setCreatedBy("tester");
        food.setUpdatedBy("tester");
        food.setUpdatedAt(java.time.LocalDateTime.now());

        AtomicReference<Food> savedRef = new AtomicReference<>();
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);
        when(foodRepository.save(any(Food.class))).thenAnswer(invocation -> {
            Food saved = invocation.getArgument(0);
            saved.setId(20L);
            savedRef.set(saved);
            return saved;
        });
        when(recipeRepository.findDetailedByFoodId(20L)).thenReturn(List.of());

        FoodDTO result = foodService.createFood(new FoodDTO(null, "Khao Pad", "Rice",
                null, null, null, null, null, List.of()));

        assertEquals("tester", savedRef.get().getCreatedBy());
        assertEquals("tester", savedRef.get().getUpdatedBy());
        assertNotNull(savedRef.get().getUpdatedAt());
        assertEquals("tester", result.getCreatedBy());
        assertEquals("tester", result.getUpdatedBy());
    }

    @Test
    void createFoodShouldCreateRecipeVersionsWhenProvided() {
        User user = new User();
        user.setUserName("tester");
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
        when(recipeRepository.findDetailedByFoodId(12L)).thenReturn(List.of());
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        foodService.createFood(new FoodDTO(null, "Som Tum", "Salad",
                "https://img.example/som-tum.jpg", null, null, null, null, List.of(recipeDTO)));

        verify(recipeService).createRecipe(any(RecipeDTO.class));
    }

    @Test
    void createFoodShouldThrowWhenNameAlreadyExists() {
        User user = new User();
        user.setUserName("tester");
        when(foodRepository.existsByNameIgnoreCase("Ramen")).thenReturn(true);
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        assertThrows(DuplicateResourceException.class,
                () -> foodService.createFood(new FoodDTO(null, "Ramen", "Noodle",
                        "https://img.example/ramen.jpg", null, null, null, null, List.of())));

        verify(foodRepository, never()).save(any(Food.class));
    }

    @Test
    void getAllFoodsShouldUseBatchCountsAndNotMapRecipes() {
        Food food1 = new Food();
        food1.setId(1L);
        food1.setName("Ramen");

        Food food2 = new Food();
        food2.setId(2L);
        food2.setName("Tom Yum");

        when(foodRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(food1, food2), PageRequest.of(0, 20), 2));
        when(recipeRepository.countByFoodIds(List.of(1L, 2L)))
                .thenReturn(List.of(projection(1L, 2), projection(2L, 1)));

        Page<FoodDTO> result = foodService.getAllFoods(PageRequest.of(0, 20));

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getContent().get(0).getRecipeCount());
        assertEquals(1, result.getContent().get(1).getRecipeCount());
        assertTrue(result.getContent().get(0).getRecipes().isEmpty());
        assertTrue(result.getContent().get(1).getRecipes().isEmpty());
    }

    private FoodRecipeCountProjection projection(Long foodId, long recipeCount) {
        return new FoodRecipeCountProjection() {
            @Override
            public Long getFoodId() {
                return foodId;
            }

            @Override
            public long getRecipeCount() {
                return recipeCount;
            }
        };
    }
}
