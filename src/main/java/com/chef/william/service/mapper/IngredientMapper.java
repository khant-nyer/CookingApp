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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IngredientMapper {

    public void updateEntityFromDto(IngredientDTO dto, Ingredient entity) {
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setDescription(dto.getDescription());
        entity.setServingAmount(dto.getServingAmount());

        Unit servingUnit = dto.getServingUnit();
        if (servingUnit == null) {
            throw new BusinessException("Serving unit is required");
        }
        entity.setServingUnit(servingUnit.getAbbreviation());

        if (entity.getNutritionList() == null) {
            entity.setNutritionList(new ArrayList<>());
        }

        if (dto.getNutritionList() == null) {
            entity.getNutritionList().clear();
            return;
        }

        //on create: entity.getNutritionList() is empty -> map is empty → all will be new inserts
        //on update: load every nutrition objects from db and map them with nutrients(enum)
        Map<Nutrients, Nutrition> existingMap = new HashMap<>();
        entity.getNutritionList().stream()
                .filter(n -> n.getNutrient() != null)
                .forEach(n -> existingMap.putIfAbsent(n.getNutrient(), n));

        //retrieving nutrients from dto(client)
        // Determine which nutrient types are present in the incoming DTO
        // (used to detect removals on update; ignored on create since list is empty)
        Set<Nutrients> dtoNutrientTypes = dto.getNutritionList().stream()
                .map(NutritionDTO::getNutrient)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        //on update: filtering nutrition objects that are loaded from db based on nutrients that comes from dto(client)
        //on create: no effect (list is empty)
        entity.getNutritionList().removeIf(n -> n.getNutrient() == null || !dtoNutrientTypes.contains(n.getNutrient()));

        for (NutritionDTO nDto : dto.getNutritionList()) {
            if (nDto.getNutrient() == null) {
                continue;
            }

            Nutrition nutrition = existingMap.get(nDto.getNutrient());

            if (nutrition == null) {
                nutrition = new Nutrition();
                nutrition.setIngredient(entity);
                entity.getNutritionList().add(nutrition);
                existingMap.put(nDto.getNutrient(), nutrition);
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
            dto.setNutritionList(entity.getNutritionList().stream()
                    .map(n -> new NutritionDTO(n.getId(), n.getNutrient(), n.getValue(), n.getUnit()))
                    .collect(Collectors.toList()));
        } else {
            dto.setNutritionList(new ArrayList<>());
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
                listing.getStorePlaceId(),
                listing.getLatitude(),
                listing.getLongitude(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getInStock(),
                listing.getDistanceKm(),
                listing.getSourceProvider(),
                listing.getCapturedAt(),
                listing.getExpiresAt()
        );
    }
}
