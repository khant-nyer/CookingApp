package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.service.ingredient.IngredientSearchService;
import com.chef.william.service.mapper.IngredientMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private IngredientMapper ingredientMapper;

    @Mock
    private IngredientSearchService ingredientSearchService;


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
        update.setNutritionList(List.of(
                new NutritionDTO(null, Nutrients.PROTEIN, 3.5, "g"),
                new NutritionDTO(null, Nutrients.CALORIES, 18.0, "kcal")
        ));

        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingredientMapper.toDto(any(Ingredient.class))).thenAnswer(invocation -> {
            Ingredient source = invocation.getArgument(0);
            IngredientDTO mapped = new IngredientDTO();
            mapped.setId(source.getId());
            mapped.setName(source.getName());
            mapped.setServingAmount(source.getServingAmount());
            mapped.setServingUnit(Unit.fromAbbreviation(source.getServingUnit()));
            List<NutritionDTO> nutrition = source.getNutritionList().stream()
                    .map(n -> new NutritionDTO(n.getId(), n.getNutrient(), n.getValue(), n.getUnit()))
                    .toList();
            mapped.setNutritionList(nutrition);
            return mapped;
        });

        IngredientDTO result = ingredientService.updateIngredient(1L, update);

        assertEquals(2, ingredient.getNutritionList().size());
        assertEquals(2, result.getNutritionList().size());
    }

    @Test
    void getIngredientByIdMapsServingUnitFromAbbreviation() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(5L);
        ingredient.setName("Salt");
        ingredient.setServingAmount(100.0);
        ingredient.setServingUnit("g");
        ingredient.setImageUrl("https://img.example/salt.jpg");

        when(ingredientRepository.findById(5L)).thenReturn(Optional.of(ingredient));
        IngredientDTO mapped = new IngredientDTO();
        mapped.setId(5L);
        mapped.setName("Salt");
        mapped.setServingAmount(100.0);
        mapped.setServingUnit(Unit.G);
        mapped.setImageUrl("https://img.example/salt.jpg");
        when(ingredientMapper.toDto(ingredient)).thenReturn(mapped);

        IngredientDTO dto = ingredientService.getIngredientById(5L);

        assertEquals(Unit.G, dto.getServingUnit());
        assertEquals("https://img.example/salt.jpg", dto.getImageUrl());
    }

    @Test
    void getIngredientByIdThrowsWhenServingUnitIsUnsupported() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(6L);
        ingredient.setName("Mystery");
        ingredient.setServingAmount(1.0);
        ingredient.setServingUnit("gramz");

        when(ingredientRepository.findById(6L)).thenReturn(Optional.of(ingredient));
        when(ingredientMapper.toDto(ingredient))
                .thenThrow(new BusinessException("Unsupported serving unit found in database: gramz"));

        assertThrows(BusinessException.class, () -> ingredientService.getIngredientById(6L));
    }


    @Test
    void searchIngredientByNutrientThrowsForInvalidNutrient() {
        when(ingredientSearchService.searchByNutrient(eq("INVALID"), eq(1.0)))
                .thenThrow(new BusinessException("Invalid nutrient"));

        assertThrows(BusinessException.class,
                () -> ingredientService.searchIngredientByNutrient("INVALID", 1.0));
    }

    @Test
    void createIngredientsShouldSaveAllInBatch() {
        IngredientDTO saltDto = new IngredientDTO();
        saltDto.setName("Salt");
        saltDto.setServingAmount(100.0);
        saltDto.setServingUnit(Unit.G);

        IngredientDTO pepperDto = new IngredientDTO();
        pepperDto.setName("Pepper");
        pepperDto.setServingAmount(100.0);
        pepperDto.setServingUnit(Unit.G);

        when(ingredientRepository.findExistingNormalizedNames(Set.of("salt", "pepper"))).thenReturn(Set.of());
        when(ingredientRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ingredientMapper.toDto(any(Ingredient.class))).thenAnswer(invocation -> {
            Ingredient source = invocation.getArgument(0);
            IngredientDTO mapped = new IngredientDTO();
            mapped.setName(source.getName());
            mapped.setServingAmount(source.getServingAmount());
            mapped.setServingUnit(Unit.fromAbbreviation(source.getServingUnit()));
            return mapped;
        });

        List<IngredientDTO> result = ingredientService.createIngredients(List.of(saltDto, pepperDto));

        assertEquals(2, result.size());
        verify(ingredientRepository).saveAll(any());
    }


    @Test
    void createIngredientsShouldThrowWhenPayloadContainsNullItem() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ingredientService.createIngredients(Arrays.asList((IngredientDTO) null)));

        assertEquals("Ingredient payload item must not be null", ex.getMessage());
    }

    @Test
    void createIngredientsShouldThrowWhenNameIsBlank() {
        IngredientDTO dto = new IngredientDTO();
        dto.setName("   ");
        dto.setServingAmount(100.0);
        dto.setServingUnit(Unit.G);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ingredientService.createIngredients(List.of(dto)));

        assertEquals("Ingredient name is required in bulk payload", ex.getMessage());
    }

    @Test
    void createIngredientsShouldThrowWhenAnyNameAlreadyExists() {
        IngredientDTO dto = new IngredientDTO();
        dto.setName("Salt");
        dto.setServingAmount(100.0);
        dto.setServingUnit(Unit.G);

        when(ingredientRepository.findExistingNormalizedNames(Set.of("salt"))).thenReturn(Set.of("salt"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ingredientService.createIngredients(List.of(dto)));

        assertEquals("Ingredient already exists: Salt", ex.getMessage());
    }
}
