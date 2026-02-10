package com.chef.william.service.recipe;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Instruction;
import com.chef.william.model.Recipe;
import com.chef.william.model.RecipeIngredient;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.IngredientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeMergeServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeMergeService recipeMergeService;

    @Test
    void mergeIngredientsThrowsWhenIngredientIdsAreDuplicated() {
        RecipeDTO dto = new RecipeDTO();
        dto.setIngredients(List.of(
                new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null),
                new RecipeIngredientDTO(null, 1L, null, 2.0, Unit.G, null)
        ));

        assertThrows(BusinessException.class, () -> recipeMergeService.mergeIngredients(new Recipe(), dto));
    }

    @Test
    void mergeInstructionsThrowsWhenStepsAreDuplicated() {
        RecipeDTO dto = new RecipeDTO();
        dto.setInstructions(List.of(
                InstructionDTO.builder().step(1).description("First").build(),
                InstructionDTO.builder().step(1).description("Duplicate").build()
        ));

        assertThrows(BusinessException.class, () -> recipeMergeService.mergeInstructions(new Recipe(), dto));
    }

    @Test
    void mergeIngredientsAndInstructionsUpdatesAndAddsItems() {
        Ingredient salt = new Ingredient();
        salt.setId(1L);
        salt.setName("Salt");

        Ingredient pepper = new Ingredient();
        pepper.setId(2L);
        pepper.setName("Pepper");

        Recipe recipe = new Recipe();

        RecipeIngredient existingRi = new RecipeIngredient();
        existingRi.setRecipe(recipe);
        existingRi.setIngredient(salt);
        existingRi.setQuantity(1.0);
        existingRi.setUnit(Unit.G);

        Instruction existingIns = new Instruction();
        existingIns.setRecipe(recipe);
        existingIns.setStep(1);
        existingIns.setDescription("Old");

        recipe.getRecipeIngredients().add(existingRi);
        recipe.getInstructions().add(existingIns);

        RecipeDTO update = new RecipeDTO();
        update.setIngredients(List.of(
                new RecipeIngredientDTO(null, 1L, null, 3.0, Unit.KG, "updated"),
                new RecipeIngredientDTO(null, 2L, null, 2.0, Unit.G, "added")
        ));
        update.setInstructions(List.of(
                InstructionDTO.builder().step(1).description("Updated step").build(),
                InstructionDTO.builder().step(2).description("New step").build()
        ));

        when(ingredientRepository.findById(2L)).thenReturn(Optional.of(pepper));

        recipeMergeService.mergeIngredients(recipe, update);
        recipeMergeService.mergeInstructions(recipe, update);

        assertEquals(2, recipe.getRecipeIngredients().size());
        assertEquals(3.0, existingRi.getQuantity());
        assertEquals(Unit.KG, existingRi.getUnit());
        assertEquals(2, recipe.getInstructions().size());
        assertEquals("Updated step", existingIns.getDescription());
    }
}
