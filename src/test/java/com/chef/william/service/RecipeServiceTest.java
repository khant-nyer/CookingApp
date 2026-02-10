package com.chef.william.service;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Instruction;
import com.chef.william.model.Recipe;
import com.chef.william.model.RecipeIngredient;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.repository.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private IngredientRepository ingredientRepository;

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void createRecipeThrowsWhenIngredientIdsAreDuplicated() {
        RecipeDTO dto = new RecipeDTO();
        dto.setTitle("Pasta");
        dto.setIngredients(List.of(
                new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null),
                new RecipeIngredientDTO(null, 1L, null, 2.0, Unit.G, null)
        ));
        dto.setInstructions(List.of(InstructionDTO.builder().step(1).description("Mix").build()));

        assertThrows(BusinessException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipeThrowsWhenInstructionStepsAreDuplicated() {
        RecipeDTO dto = new RecipeDTO();
        dto.setTitle("Pasta");
        dto.setIngredients(List.of(new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null)));
        dto.setInstructions(List.of(
                InstructionDTO.builder().step(1).description("First").build(),
                InstructionDTO.builder().step(1).description("Duplicate").build()
        ));

        assertThrows(BusinessException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void updateRecipeMergesIngredientsAndInstructions() {
        Ingredient salt = new Ingredient();
        salt.setId(1L);
        salt.setName("Salt");

        Ingredient pepper = new Ingredient();
        pepper.setId(2L);
        pepper.setName("Pepper");

        Recipe recipe = new Recipe();
        recipe.setId(10L);
        recipe.setTitle("Old title");

        RecipeIngredient existingRi = new RecipeIngredient();
        existingRi.setId(100L);
        existingRi.setRecipe(recipe);
        existingRi.setIngredient(salt);
        existingRi.setQuantity(1.0);
        existingRi.setUnit(Unit.G);

        RecipeIngredient removedRi = new RecipeIngredient();
        removedRi.setId(101L);
        removedRi.setRecipe(recipe);
        removedRi.setIngredient(pepper);
        removedRi.setQuantity(1.0);
        removedRi.setUnit(Unit.G);

        recipe.getRecipeIngredients().add(existingRi);
        recipe.getRecipeIngredients().add(removedRi);

        Instruction existingInstruction = new Instruction();
        existingInstruction.setId(200L);
        existingInstruction.setRecipe(recipe);
        existingInstruction.setStep(1);
        existingInstruction.setDescription("Old");

        Instruction removedInstruction = new Instruction();
        removedInstruction.setId(201L);
        removedInstruction.setRecipe(recipe);
        removedInstruction.setStep(2);
        removedInstruction.setDescription("Remove");

        recipe.getInstructions().add(existingInstruction);
        recipe.getInstructions().add(removedInstruction);

        RecipeDTO update = new RecipeDTO();
        update.setTitle("New title");
        update.setDescription("New description");
        update.setIngredients(List.of(
                new RecipeIngredientDTO(null, 1L, null, 3.0, Unit.KG, "updated"),
                new RecipeIngredientDTO(null, 2L, null, 2.0, Unit.G, "added back")
        ));
        update.setInstructions(List.of(
                InstructionDTO.builder().step(1).description("Updated step").build(),
                InstructionDTO.builder().step(3).description("New step").build()
        ));

        when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
        when(ingredientRepository.findById(2L)).thenReturn(Optional.of(pepper));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDTO result = recipeService.updateRecipe(10L, update);

        assertEquals("New title", result.getTitle());
        assertEquals(2, result.getIngredients().size());
        assertEquals(2, recipe.getRecipeIngredients().size());
        assertEquals(3.0, existingRi.getQuantity());
        assertEquals(Unit.KG, existingRi.getUnit());

        assertEquals(2, recipe.getInstructions().size());
        assertEquals("Updated step", existingInstruction.getDescription());
        assertEquals(List.of(1, 3), result.getInstructions().stream().map(InstructionDTO::getStep).toList());
    }

    @Test
    void createRecipeAssignsFoodWhenFoodIdProvided() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Salt");

        Food food = new Food();
        food.setId(7L);
        food.setName("Pad Kra Pao");

        RecipeDTO dto = new RecipeDTO();
        dto.setTitle("Pad Kra Pao");
        dto.setDescription("Classic");
        dto.setFoodId(7L);
        dto.setIngredients(List.of(new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null)));
        dto.setInstructions(List.of(InstructionDTO.builder().step(1).description("Cook").build()));

        when(foodRepository.findById(7L)).thenReturn(Optional.of(food));
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        RecipeDTO result = recipeService.createRecipe(dto);

        assertEquals(7L, result.getFoodId());
        assertEquals("Pad Kra Pao", result.getFoodName());
    }

    @Test
    void createRecipeThrowsWhenFoodIdNotFound() {
        RecipeDTO dto = new RecipeDTO();
        dto.setTitle("No Food");
        dto.setFoodId(404L);
        dto.setIngredients(List.of(new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null)));
        dto.setInstructions(List.of(InstructionDTO.builder().step(1).description("Cook").build()));

        when(foodRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.createRecipe(dto));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void updateRecipeClearsFoodWhenFoodIdRemoved() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Garlic");

        Food existingFood = new Food();
        existingFood.setId(5L);
        existingFood.setName("Soup");

        Recipe recipe = new Recipe();
        recipe.setId(11L);
        recipe.setTitle("Soup v1");
        recipe.setFood(existingFood);

        RecipeIngredient ri = new RecipeIngredient();
        ri.setId(70L);
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(1.0);
        ri.setUnit(Unit.G);
        recipe.getRecipeIngredients().add(ri);

        Instruction ins = new Instruction();
        ins.setId(80L);
        ins.setRecipe(recipe);
        ins.setStep(1);
        ins.setDescription("stir");
        recipe.getInstructions().add(ins);

        RecipeDTO update = new RecipeDTO();
        update.setTitle("Soup v2");
        update.setDescription("No linked food");
        update.setFoodId(null);
        update.setIngredients(List.of(new RecipeIngredientDTO(null, 1L, null, 2.0, Unit.G, null)));
        update.setInstructions(List.of(InstructionDTO.builder().step(1).description("stir more").build()));

        when(recipeRepository.findById(11L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeDTO result = recipeService.updateRecipe(11L, update);

        assertNull(result.getFoodId());
        assertNull(result.getFoodName());
    }
}
