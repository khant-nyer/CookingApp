package com.chef.william.service;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.model.User;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.auth.CurrentUserService;
import com.chef.william.service.mapper.RecipeMapper;
import com.chef.william.service.recipe.RecipeMergeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void createRecipeThrowsWhenFoodIdNotFound() {
        User user = new User();
        user.setUserName("chef");
        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v1");
        dto.setFoodId(404L);
        dto.setIngredients(List.of());
        dto.setInstructions(List.of());

        when(foodRepository.findById(404L)).thenReturn(Optional.empty());
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        assertThrows(ResourceNotFoundException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipeThrowsWhenVersionAlreadyExists() {
        User user = new User();
        user.setUserName("chef");
        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v1");

        when(recipeRepository.existsByVersion("v1")).thenReturn(true);
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        assertThrows(DuplicateResourceException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipeAssignsFoodWhenFoodIdProvided() {
        User user = new User();
        user.setUserName("chef");
        Food food = new Food();
        food.setId(7L);
        food.setName("Pad Kra Pao");

        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v1");
        dto.setDescription("Classic");
        dto.setFoodId(7L);

        when(recipeRepository.existsByVersion("v1")).thenReturn(false);
        when(foodRepository.findById(7L)).thenReturn(Optional.of(food));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeMapper.toDto(any(Recipe.class))).thenReturn(new RecipeDTO());
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);

        recipeService.createRecipe(dto);

        verify(recipeMergeService).mergeIngredients(any(Recipe.class), any(RecipeDTO.class));
        verify(recipeMergeService).mergeInstructions(any(Recipe.class), any(RecipeDTO.class));
    }

    @Test
    void createRecipeShouldPopulateAuditFields() {
        User user = new User();
        user.setUserName("chef");
        RecipeDTO dto = new RecipeDTO();
        dto.setVersion("v3");
        dto.setDescription("Audit check");

        AtomicReference<Recipe> savedRef = new AtomicReference<>();
        when(recipeRepository.existsByVersion("v3")).thenReturn(false);
        when(currentUserService.getRequiredCurrentUser()).thenReturn(user);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            savedRef.set(saved);
            return saved;
        });
        when(recipeMapper.toDto(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe source = invocation.getArgument(0);
            RecipeDTO mapped = new RecipeDTO();
            mapped.setCreatedBy(source.getCreatedBy());
            mapped.setUpdatedBy(source.getUpdatedBy());
            mapped.setUpdatedAt(source.getUpdatedAt());
            return mapped;
        });

        RecipeDTO result = recipeService.createRecipe(dto);

        assertEquals("chef", savedRef.get().getCreatedBy());
        assertEquals("chef", savedRef.get().getUpdatedBy());
        assertNotNull(savedRef.get().getUpdatedAt());
        assertEquals("chef", result.getCreatedBy());
    }

    @Test
    void updateRecipeThrowsWhenChangingToExistingVersion() {
        Recipe existingRecipe = new Recipe();
        existingRecipe.setId(1L);
        existingRecipe.setVersion("v1");

        RecipeDTO update = new RecipeDTO();
        update.setVersion("v2");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(existingRecipe));
        when(recipeRepository.existsByVersion("v2")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> recipeService.updateRecipe(1L, update));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void getAllRecipesShouldReturnMappedPage() {
        Recipe recipe = new Recipe();
        recipe.setId(11L);
        recipe.setVersion("v1");

        RecipeDTO dto = new RecipeDTO();
        dto.setId(11L);
        dto.setVersion("v1");

        when(recipeRepository.findAllIds(PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(11L), PageRequest.of(0, 5), 1));
        when(recipeRepository.findDetailedByIdIn(List.of(11L))).thenReturn(List.of(recipe));
        when(recipeMapper.toDto(recipe)).thenReturn(dto);

        Page<RecipeDTO> result = recipeService.getAllRecipes(PageRequest.of(0, 5));

        assertEquals(1, result.getTotalElements());
        assertEquals("v1", result.getContent().getFirst().getVersion());
    }

}
