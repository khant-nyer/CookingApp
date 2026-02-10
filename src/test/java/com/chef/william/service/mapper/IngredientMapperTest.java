package com.chef.william.service.mapper;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.enums.Unit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngredientMapperTest {

    private final IngredientMapper ingredientMapper = new IngredientMapper();

    @Test
    void updateEntityFromDtoHandlesExistingDuplicateNutrientRows() {
        Ingredient entity = new Ingredient();
        entity.setNutritionList(new ArrayList<>());

        Nutrition duplicateProtein1 = new Nutrition();
        duplicateProtein1.setNutrient(Nutrients.PROTEIN);
        duplicateProtein1.setValue(1.0);
        duplicateProtein1.setUnit("g");

        Nutrition duplicateProtein2 = new Nutrition();
        duplicateProtein2.setNutrient(Nutrients.PROTEIN);
        duplicateProtein2.setValue(2.0);
        duplicateProtein2.setUnit("g");

        entity.getNutritionList().add(duplicateProtein1);
        entity.getNutritionList().add(duplicateProtein2);

        IngredientDTO dto = new IngredientDTO();
        dto.setName("Egg");
        dto.setServingAmount(100.0);
        dto.setServingUnit(Unit.G);
        dto.setNutrients(List.of(new NutritionDTO(null, Nutrients.PROTEIN, 12.3, "g")));

        assertDoesNotThrow(() -> ingredientMapper.updateEntityFromDto(dto, entity));
        assertEquals(1, entity.getNutritionList().size());
        assertEquals(12.3, entity.getNutritionList().get(0).getValue());
    }

    @Test
    void updateEntityFromDtoThrowsWhenServingUnitIsMissing() {
        Ingredient entity = new Ingredient();
        IngredientDTO dto = new IngredientDTO();
        dto.setName("Milk");
        dto.setServingAmount(100.0);
        dto.setServingUnit(null);

        assertThrows(BusinessException.class, () -> ingredientMapper.updateEntityFromDto(dto, entity));
    }
}
