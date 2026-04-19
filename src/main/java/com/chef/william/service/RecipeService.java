package com.chef.william.service;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.model.User;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.auth.CurrentUserService;
import com.chef.william.service.mapper.RecipeMapper;
import com.chef.william.service.recipe.RecipeMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final FoodRepository foodRepository;
    private final RecipeMergeService recipeMergeService;
    private final RecipeMapper recipeMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public RecipeDTO createRecipe(RecipeDTO recipeDTO) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        validateUniqueVersionForCreate(recipeDTO.getVersion());

        Recipe recipe = new Recipe();
        populateScalars(recipe, recipeDTO, currentUser);
        recipeMergeService.mergeIngredients(recipe, recipeDTO);
        recipeMergeService.mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return recipeMapper.toDto(recipe);
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, RecipeDTO recipeDTO) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        validateUniqueVersionForUpdate(recipe, recipeDTO.getVersion());
        populateScalars(recipe, recipeDTO, currentUser);
        recipeMergeService.mergeIngredients(recipe, recipeDTO);
        recipeMergeService.mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return recipeMapper.toDto(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeDTO getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        return recipeMapper.toDto(recipe);
    }

    @Transactional(readOnly = true)
    public Page<RecipeDTO> getAllRecipes(Pageable pageable) {
        Page<Long> idPage = recipeRepository.findAllIds(pageable);
        List<Long> ids = idPage.getContent();

        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Recipe> recipesById = new LinkedHashMap<>();
        recipeRepository.findDetailedByIdIn(ids)
                .forEach(recipe -> recipesById.put(recipe.getId(), recipe));

        List<RecipeDTO> ordered = ids.stream()
                .map(recipesById::get)
                .map(recipeMapper::toDto)
                .toList();

        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recipe not found with id: " + id);
        }
        recipeRepository.deleteById(id);
    }

    private void validateUniqueVersionForCreate(String version) {
        if (version != null && recipeRepository.existsByVersion(version)) {
            throw new DuplicateResourceException("Recipe", "version", version);
        }
    }

    private void validateUniqueVersionForUpdate(Recipe existingRecipe, String version) {
        if (version != null
                && !version.equals(existingRecipe.getVersion())
                && recipeRepository.existsByVersion(version)) {
            throw new DuplicateResourceException("Recipe", "version", version);
        }
    }

    private void populateScalars(Recipe recipe, RecipeDTO dto, User currentUser) {
        recipe.setVersion(dto.getVersion());
        recipe.setDescription(dto.getDescription());
        recipe.setUser(currentUser);

        if (dto.getFoodId() != null) {
            Food food = foodRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + dto.getFoodId()));
            recipe.setFood(food);
        } else {
            recipe.setFood(null);
        }
    }
}
