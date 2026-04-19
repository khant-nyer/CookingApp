package com.chef.william.service;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.User;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.service.auth.CurrentUserService;
import com.chef.william.service.ingredient.IngredientSearchService;
import com.chef.william.service.mapper.IngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final IngredientMapper ingredientMapper;
    private final IngredientSearchService ingredientSearchService;
    private final CurrentUserService currentUserService;

    @Transactional
    public IngredientDTO createIngredient(IngredientDTO dto) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        String auditActor = resolveAuditActor(currentUser);
        Ingredient ingredient = new Ingredient();
        ingredientMapper.updateEntityFromDto(dto, ingredient);
        ingredient.setUser(currentUser);
        ingredient.setCreatedBy(auditActor);
        ingredient.setUpdatedBy(auditActor);
        ingredient.setUpdatedAt(LocalDateTime.now());
        ingredient = ingredientRepository.save(ingredient);
        return ingredientMapper.toDto(ingredient);
    }

    @Transactional
    public List<IngredientDTO> createIngredients(List<IngredientDTO> dtos) {
        validateBulkCreatePayload(dtos);
        User currentUser = currentUserService.getRequiredCurrentUser();
        String auditActor = resolveAuditActor(currentUser);
        List<Ingredient> ingredients = dtos.stream()
                .map(dto -> {
                    if (dto == null) {
                        throw new BusinessException("Ingredient payload item must not be null");
                    }
                    Ingredient ingredient = new Ingredient();
                    ingredientMapper.updateEntityFromDto(dto, ingredient);
                    ingredient.setUser(currentUser);
                    ingredient.setCreatedBy(auditActor);
                    ingredient.setUpdatedBy(auditActor);
                    ingredient.setUpdatedAt(LocalDateTime.now());
                    return ingredient;
                })
                .toList();

        return ingredientRepository.saveAll(ingredients).stream()
                .map(ingredientMapper::toDto)
                .toList();
    }

    @Transactional
    public IngredientDTO updateIngredient(Long id, IngredientDTO dto) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        String auditActor = resolveAuditActor(currentUser);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        ingredientMapper.updateEntityFromDto(dto, ingredient);
        ingredient.setUser(currentUser);
        if (ingredient.getCreatedBy() == null || ingredient.getCreatedBy().isBlank()) {
            ingredient.setCreatedBy(auditActor);
        }
        ingredient.setUpdatedBy(auditActor);
        ingredient.setUpdatedAt(LocalDateTime.now());
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
    public Page<IngredientDTO> getAllIngredients(Pageable pageable) {
        return ingredientRepository.findAll(pageable)
                .map(ingredientMapper::toDto);
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
    public List<IngredientDTO> searchIngredientsByName(String name) {
        return ingredientSearchService.searchByName(name);
    }

    @Transactional(readOnly = true)
    public List<IngredientDTO> searchIngredientByNutrient(String nutrientStr, Double minValue) {
        return ingredientSearchService.searchByNutrient(nutrientStr, minValue);
    }

    private void validateBulkCreatePayload(List<IngredientDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BusinessException("Ingredient bulk payload must not be empty");
        }

        Set<String> normalizedNames = new HashSet<>();
        for (IngredientDTO dto : dtos) {
            if (dto == null) {
                throw new BusinessException("Ingredient payload item must not be null");
            }

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                throw new BusinessException("Ingredient name is required in bulk payload");
            }

            String normalized = dto.getName().trim().toLowerCase();
            if (!normalizedNames.add(normalized)) {
                throw new BusinessException("Duplicate ingredient name in bulk payload: " + dto.getName());
            }
        }

        Set<String> existingNames = ingredientRepository.findExistingNormalizedNames(normalizedNames);
        if (!existingNames.isEmpty()) {
            String duplicated = dtos.stream()
                    .filter(dto -> dto != null && dto.getName() != null)
                    .map(dto -> dto.getName().trim())
                    .filter(name -> existingNames.contains(name.toLowerCase()))
                    .findFirst()
                    .orElse(existingNames.iterator().next());
            throw new BusinessException("Ingredient already exists: " + duplicated);
        }
    }

    private String resolveAuditActor(User currentUser) {
        if (currentUser.getUserName() != null && !currentUser.getUserName().isBlank()) {
            return currentUser.getUserName();
        }
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            return currentUser.getEmail();
        }
        if (currentUser.getCognitoSub() != null && !currentUser.getCognitoSub().isBlank()) {
            return currentUser.getCognitoSub();
        }
        throw new BusinessException("Authenticated user has no usable identifier for audit fields");
    }
}
