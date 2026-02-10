package com.chef.william.service.ingredient;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.service.mapper.IngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientSearchService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ingredientRepository.findAll().stream().map(ingredientMapper::toDto).toList();
        }
        return ingredientRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(ingredientMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchByNutrient(String nutrientStr, Double minValue) {
        if (nutrientStr == null || nutrientStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Nutrient parameter is required");
        }

        Nutrients nutrient;
        try {
            nutrient = Nutrients.valueOf(nutrientStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid nutrient: " + nutrientStr +
                    ". Valid values: " + Arrays.toString(Nutrients.values()));
        }

        double min = (minValue != null) ? minValue : 0.0;

        return ingredientRepository.findByNutrientAndMinValue(nutrient, min)
                .stream()
                .map(ingredientMapper::toDto)
                .toList();
    }
}
