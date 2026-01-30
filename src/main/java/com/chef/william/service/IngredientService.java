package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Unit;
import com.chef.william.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    @Transactional
    public IngredientDTO createIngredient(IngredientDTO dto) {
        Ingredient ingredient = new Ingredient();
        mapToEntity(dto, ingredient);
        ingredient = ingredientRepository.save(ingredient);
        return mapToDto(ingredient);
    }

    @Transactional
    public IngredientDTO updateIngredient(Long id, IngredientDTO dto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        mapToEntity(dto, ingredient);
        ingredient = ingredientRepository.save(ingredient);
        return mapToDto(ingredient);
    }

    @Transactional(readOnly = true)
    public IngredientDTO getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));
        return mapToDto(ingredient);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteIngredient(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        if (!ingredient.getRecipeIngredients().isEmpty()) {
            throw new BusinessException("Cannot delete ingredient '" + ingredient.getName() +
                    "' because it is used in one or more recipes.");
        }

        ingredientRepository.delete(ingredient);
    }

    // Helper: DTO → Entity (used for both create and update)
    private void mapToEntity(IngredientDTO dto, Ingredient entity) {
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setDescription(dto.getDescription());
        entity.setServingAmount(dto.getServingAmount());

        // Handle servingUnit (assuming entity stores String abbreviation)
        entity.setServingUnit(dto.getServingUnit().getAbbreviation());

        // === SMART NUTRITION MERGE ===
        if (dto.getNutrients() != null) {
            // Map existing nutrients by type for quick lookup
            Map<Nutrients, Nutrition> existingMap = entity.getNutritionList().stream()
                    .collect(Collectors.toMap(Nutrition::getNutrient, n -> n));

            // Collect nutrient types from DTO (to know what to keep)
            Set<Nutrients> dtoNutrientTypes = dto.getNutrients().stream()
                    .map(NutritionDTO::getNutrient)
                    .collect(Collectors.toSet());

            // Remove existing nutrients not present in DTO (orphanRemoval will delete them)
            entity.getNutritionList().removeIf(n -> !dtoNutrientTypes.contains(n.getNutrient()));

            // Merge/Update/Add from DTO
            for (NutritionDTO nDto : dto.getNutrients()) {
                Nutrition nutrition = existingMap.get(nDto.getNutrient());

                if (nutrition == null) {
                    // New nutrient
                    nutrition = new Nutrition();
                    nutrition.setIngredient(entity);  // Bidirectional link
                    entity.getNutritionList().add(nutrition);
                }

                // Update fields (for both existing and new)
                nutrition.setNutrient(nDto.getNutrient());
                nutrition.setValue(nDto.getValue());
                nutrition.setUnit(nDto.getUnit());
            }
        } else {
            // If nutrients null/empty in DTO → clear all
            entity.getNutritionList().clear();
        }
    }

    // Helper: Entity → DTO
    private IngredientDTO mapToDto(Ingredient entity) {
        IngredientDTO dto = new IngredientDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setServingAmount(entity.getServingAmount());
        dto.setServingUnit(Unit.valueOf(entity.getServingUnit().toUpperCase())); // Adjust if needed

        dto.setNutrients(entity.getNutritionList().stream()
                .map(n -> {
                    NutritionDTO nDto = new NutritionDTO();
                    nDto.setId(n.getId());
                    nDto.setNutrient(n.getNutrient());
                    nDto.setValue(n.getValue());
                    nDto.setUnit(n.getUnit());
                    return nDto;
                })
                .collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchIngredientsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllIngredients();  // Or limit to recent/top—optional
        }
        return ingredientRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchIngredientByNutrient(String nutrientStr, Double minValue) {
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
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}