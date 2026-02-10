package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.NutritionDTO;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.dto.IngredientStoreListingDTO;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.model.Ingredient;
import com.chef.william.model.IngredientStoreListing;
import com.chef.william.model.enums.Nutrients;
import com.chef.william.model.Nutrition;
import com.chef.william.model.enums.Unit;
import com.chef.william.model.User;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.repository.IngredientStoreListingRepository;
import com.chef.william.repository.UserRepository;
import com.chef.william.service.crawler.SupermarketCrawlerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientStoreListingRepository ingredientStoreListingRepository;
    private final CitySupermarketRepository citySupermarketRepository;
    private final UserRepository userRepository;
    private final SupermarketCrawlerClient supermarketCrawlerClient;

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
        Unit servingUnit = Unit.fromAbbreviation(entity.getServingUnit());
        if (servingUnit == null) {
            throw new BusinessException("Unsupported serving unit stored for ingredient id " + entity.getId() +
                    ": " + entity.getServingUnit());
        }
        dto.setServingUnit(servingUnit);

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

        dto.setNearbyStoreListings(entity.getStoreListings().stream()
                .map(this::mapStoreListingToDto)
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

        return dto;
    }

    private IngredientStoreListingDTO mapStoreListingToDto(IngredientStoreListing listing) {
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


    @Transactional(readOnly = true)
    public List<IngredientStoreListingDTO> getIngredientStoreLocations(Long ingredientId) {
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new ResourceNotFoundException("Ingredient not found with id: " + ingredientId);
        }

        return ingredientStoreListingRepository
                .findActiveListingsByIngredientId(ingredientId, LocalDateTime.now())
                .stream()
                .map(this::mapStoreListingToDto)
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
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupermarketDiscoveryDTO> discoverPopularSupermarkets(Long userId, String city, String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new BusinessException("Ingredient name is required for supermarket discovery");
        }

        String effectiveCity = resolveCity(userId, city);
        List<CitySupermarket> supermarkets = citySupermarketRepository.findByCityIgnoreCase(effectiveCity.trim());

        if (supermarkets.isEmpty()) {
            return List.of();
        }

        List<SupermarketDiscoveryDTO> results = new ArrayList<>();
        for (CitySupermarket market : supermarkets) {
            String searchUrl = buildCatalogUrl(market.getCatalogSearchUrl(), ingredientName);
            boolean matched = supermarketCrawlerClient.webpageContainsIngredient(searchUrl, ingredientName);

            results.add(new SupermarketDiscoveryDTO(
                    effectiveCity,
                    market.getSupermarketName(),
                    market.getOfficialWebsite(),
                    searchUrl,
                    matched,
                    matched ? "OFFICIAL_WEB_CRAWL" : "NO_MATCH_ON_CRAWL",
                    LocalDateTime.now()
            ));
        }

        return results.stream()
                .filter(SupermarketDiscoveryDTO::isIngredientMatched)
                .toList();
    }

    private String resolveCity(Long userId, String city) {
        if (city != null && !city.trim().isEmpty()) {
            return city.trim();
        }

        if (userId == null) {
            throw new BusinessException("City is required when userId is not provided");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getCity() == null || user.getCity().isBlank()) {
            throw new BusinessException("User city is not set for user id: " + userId);
        }

        return user.getCity().trim();
    }

    private String buildCatalogUrl(String baseCatalogUrl, String ingredientName) {
        if (baseCatalogUrl == null || baseCatalogUrl.isBlank()) {
            return "";
        }

        String encodedIngredient = URLEncoder.encode(ingredientName.trim(), StandardCharsets.UTF_8);
        if (baseCatalogUrl.contains("{ingredient}")) {
            return baseCatalogUrl.replace("{ingredient}", encodedIngredient);
        }

        if (baseCatalogUrl.contains("?")) {
            return baseCatalogUrl + "&q=" + encodedIngredient;
        }

        return baseCatalogUrl + "?q=" + encodedIngredient;
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