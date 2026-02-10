package com.chef.william.service.mapper;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.IngredientStoreListingDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.IngredientStoreListing;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.enums.Unit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IngredientMapper {

    public void updateEntityFromDto(IngredientDTO dto, Ingredient entity) {
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setDescription(dto.getDescription());
        entity.setServingAmount(dto.getServingAmount());
        entity.setServingUnit(dto.getServingUnit().getAbbreviation());

        if (dto.getNutrients() == null) {
            entity.getNutritionList().clear();
            return;
        }

        Map<Nutrients, Nutrition> existingMap = entity.getNutritionList().stream()
                .collect(Collectors.toMap(Nutrition::getNutrient, n -> n));

        Set<Nutrients> dtoNutrientTypes = dto.getNutrients().stream()
                .map(NutritionDTO::getNutrient)
                .collect(Collectors.toSet());

        entity.getNutritionList().removeIf(n -> !dtoNutrientTypes.contains(n.getNutrient()));

        for (NutritionDTO nDto : dto.getNutrients()) {
            Nutrition nutrition = existingMap.get(nDto.getNutrient());

            if (nutrition == null) {
                nutrition = new Nutrition();
                nutrition.setIngredient(entity);
                entity.getNutritionList().add(nutrition);
            }

            nutrition.setNutrient(nDto.getNutrient());
            nutrition.setValue(nDto.getValue());
            nutrition.setUnit(nDto.getUnit());
        }
    }

    public IngredientDTO toDto(Ingredient entity) {
        IngredientDTO dto = new IngredientDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setServingAmount(entity.getServingAmount());

        Unit servingUnit = Unit.fromAbbreviation(entity.getServingUnit());
        if (servingUnit == null) {
            throw new BusinessException("Unsupported serving unit found in database: " + entity.getServingUnit());
        }
        dto.setServingUnit(servingUnit);

        if (entity.getNutritionList() != null) {
            dto.setNutrients(entity.getNutritionList().stream()
                    .map(n -> new NutritionDTO(n.getId(), n.getNutrient(), n.getValue(), n.getUnit()))
                    .collect(Collectors.toList()));
        } else {
            dto.setNutrients(new ArrayList<>());
        }

        if (entity.getStoreListings() != null) {
            dto.setNearbyStoreListings(entity.getStoreListings().stream()
                    .map(this::toStoreListingDto)
                    .sorted((left, right) -> {
                        if (left.getDistanceKm() == null && right.getDistanceKm() == null) {
                            return 0;
                        }
                        if (left.getDistanceKm() == null) {
                            return 1;
                        }
                        if (right.getDistanceKm() == null) {
                            return -1;
                        }
                        return left.getDistanceKm().compareTo(right.getDistanceKm());
                    })
                    .collect(Collectors.toList()));
        } else {
            dto.setNearbyStoreListings(new ArrayList<>());
        }

        return dto;
    }

    public IngredientStoreListingDTO toStoreListingDto(IngredientStoreListing listing) {
        return new IngredientStoreListingDTO(
                listing.getId(),
                listing.getStoreName(),
                listing.getStoreAddress(),
                listing.getDistanceKm(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getInStock(),
                listing.getCapturedAt(),
                listing.getExpiresAt(),
                listing.getSourceType(),
                listing.getProductUrl(),
                listing.getExternalListingId(),
                listing.getNotes()
        );
    }
}
