package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.IngredientStoreListingDTO;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Ingredient;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.repository.IngredientStoreListingRepository;
import com.chef.william.service.ingredient.IngredientDiscoveryFacade;
import com.chef.william.service.ingredient.IngredientSearchService;
import com.chef.william.service.mapper.IngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientStoreListingRepository ingredientStoreListingRepository;
    private final IngredientMapper ingredientMapper;
    private final IngredientSearchService ingredientSearchService;
    private final IngredientDiscoveryFacade ingredientDiscoveryFacade;

    @Transactional
    public IngredientDTO createIngredient(IngredientDTO dto) {
        Ingredient ingredient = new Ingredient();
        ingredientMapper.updateEntityFromDto(dto, ingredient);
        ingredient = ingredientRepository.save(ingredient);
        return ingredientMapper.toDto(ingredient);
    }

    @Transactional
    public List<IngredientDTO> createIngredients(List<IngredientDTO> dtos) {
        validateBulkCreatePayload(dtos);
        return dtos.stream()
                .map(this::createIngredient)
                .toList();
    }

    @Transactional
    public IngredientDTO updateIngredient(Long id, IngredientDTO dto) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        ingredientMapper.updateEntityFromDto(dto, ingredient);
        ingredient = ingredientRepository.save(ingredient);
        return ingredientMapper.toDto(ingredient);
    }

    @Transactional(readOnly = true)
    public IngredientDTO getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));
        return ingredientMapper.toDto(ingredient);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(ingredientMapper::toDto)
                .toList();
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

    @Transactional(readOnly = true)
    public List<IngredientStoreListingDTO> getIngredientStoreLocations(Long ingredientId) {
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new ResourceNotFoundException("Ingredient not found with id: " + ingredientId);
        }

        return ingredientStoreListingRepository
                .findActiveListingsByIngredientId(ingredientId, LocalDateTime.now())
                .stream()
                .map(ingredientMapper::toStoreListingDto)
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

    @Transactional
    public List<SupermarketDiscoveryDTO> discoverPopularSupermarkets(Long userId, String city, String ingredientName) {
        return ingredientDiscoveryFacade.discoverPopularSupermarkets(userId, city, ingredientName);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchIngredientsByName(String name) {
        return ingredientSearchService.searchByName(name);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchIngredientByNutrient(String nutrientStr, Double minValue) {
        return ingredientSearchService.searchByNutrient(nutrientStr, minValue);
    }

    private void validateBulkCreatePayload(List<IngredientDTO> dtos) {
        Set<String> normalizedNames = new HashSet<>();
        for (IngredientDTO dto : dtos) {
            String normalized = dto.getName().trim().toLowerCase();
            if (!normalizedNames.add(normalized)) {
                throw new BusinessException("Duplicate ingredient name in bulk payload: " + dto.getName());
            }
            if (ingredientRepository.existsByNameIgnoreCase(dto.getName().trim())) {
                throw new BusinessException("Ingredient already exists: " + dto.getName());
            }
        }
    }
}
