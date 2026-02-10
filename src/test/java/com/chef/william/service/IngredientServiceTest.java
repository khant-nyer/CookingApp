package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.IngredientRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    @Test
    void updateIngredientMergesNutrientsByType() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Tomato");
        ingredient.setServingUnit("g");
        ingredient.setNutritionList(new ArrayList<>());

        Nutrition protein = new Nutrition();
        protein.setId(11L);
        protein.setIngredient(ingredient);
        protein.setNutrient(Nutrients.PROTEIN);
        protein.setValue(1.0);
        protein.setUnit("g");

        Nutrition fat = new Nutrition();
        fat.setId(12L);
        fat.setIngredient(ingredient);
        fat.setNutrient(Nutrients.FAT);
        fat.setValue(2.0);
        fat.setUnit("g");

        ingredient.getNutritionList().add(protein);
        ingredient.getNutritionList().add(fat);

        IngredientDTO update = new IngredientDTO();
        update.setName("Tomato");
        update.setCategory("Vegetable");
        update.setDescription("Fresh");
        update.setServingAmount(100.0);
        update.setServingUnit(Unit.G);
        update.setNutrients(List.of(
                new NutritionDTO(null, Nutrients.PROTEIN, 3.5, "g"),
                new NutritionDTO(null, Nutrients.CALORIES, 18.0, "kcal")
        ));

        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngredientDTO result = ingredientService.updateIngredient(1L, update);

        assertEquals(2, ingredient.getNutritionList().size());
        assertEquals(2, result.getNutrients().size());

        Nutrition updatedProtein = ingredient.getNutritionList().stream()
                .filter(n -> n.getNutrient() == Nutrients.PROTEIN)
                .findFirst()
                .orElseThrow();
        assertEquals(3.5, updatedProtein.getValue());

        boolean fatStillPresent = ingredient.getNutritionList().stream()
                .anyMatch(n -> n.getNutrient() == Nutrients.FAT);
        assertEquals(false, fatStillPresent);
    }

    @Test
    void getIngredientByIdMapsServingUnitFromAbbreviation() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(5L);
        ingredient.setName("Salt");
        ingredient.setServingAmount(100.0);
        ingredient.setServingUnit("g");

        when(ingredientRepository.findById(5L)).thenReturn(Optional.of(ingredient));

        IngredientDTO dto = ingredientService.getIngredientById(5L);

        assertEquals(Unit.G, dto.getServingUnit());
    }

    @Test
    void getIngredientByIdThrowsWhenServingUnitIsUnsupported() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(6L);
        ingredient.setName("Mystery");
        ingredient.setServingAmount(1.0);
        ingredient.setServingUnit("gramz");

        when(ingredientRepository.findById(6L)).thenReturn(Optional.of(ingredient));

        assertThrows(BusinessException.class, () -> ingredientService.getIngredientById(6L));
    }

    @Test
    void searchIngredientByNutrientThrowsForInvalidNutrient() {
        assertThrows(BusinessException.class,
                () -> ingredientService.searchIngredientByNutrient("INVALID", 1.0));
    }

}