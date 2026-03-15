package com.chef.william.service;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.DuplicateResourceException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Transactional
    public FoodDTO createFood(FoodDTO dto) {
        String normalizedName = normalizeName(dto.getName());
        if (foodRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Food", "name", normalizedName);
        }

        Food food = new Food();
        mapToEntity(dto, food);
        Food savedFood = foodRepository.save(food);
        createRecipeVersions(savedFood.getId(), dto.getRecipes());
        return mapToDto(savedFood);
    }

    @Transactional
    public FoodDTO updateFood(Long id, FoodDTO dto) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + id));

        String normalizedName = normalizeName(dto.getName());
        if (foodRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new DuplicateResourceException("Food", "name", normalizedName);
        }

        mapToEntity(dto, food);
        Food savedFood = foodRepository.save(food);
        createRecipeVersions(savedFood.getId(), dto.getRecipes());
        return mapToDto(savedFood);
    }

    @Transactional(readOnly = true)
    public FoodDTO getFoodById(Long id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + id));
        return mapToDto(food);
    }

    @Transactional(readOnly = true)
    public Page<FoodDTO> getAllFoods(Pageable pageable) {
        Page<Food> foods = foodRepository.findAll(pageable);
        if (foods.isEmpty()) {
            return foods.map(this::mapToDto);
        }

        Map<Long, Integer> recipeCountByFoodId = getRecipeCountByFoodIds(foods.stream().map(Food::getId).toList());

        return foods.map(food -> mapToSummaryDto(food, recipeCountByFoodId.getOrDefault(food.getId(), 0)));
    }

    @Transactional
    public void deleteFood(Long id) {
        if (!foodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Food not found with id: " + id);
        }
        foodRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public FoodRecipeStatusDTO getFoodRecipeStatus(Long id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + id));

        boolean hasRecipe = recipeRepository.existsByFoodId(food.getId());
        String message = hasRecipe
                ? "Recipes are available for this food"
                : "There is no recipe for this food yet, create one";

        return new FoodRecipeStatusDTO(food.getId(), food.getName(), hasRecipe, message);
    }

    private void mapToEntity(FoodDTO dto, Food entity) {
        entity.setName(normalizeName(dto.getName()));
        entity.setCategory(dto.getCategory());
        entity.setImageUrl(dto.getImageUrl());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private FoodDTO mapToDto(Food food) {
        List<RecipeDTO> recipes = recipeRepository.findByFoodId(food.getId()).stream()
                .map(recipeMapper::toDto)
                .toList();
        return new FoodDTO(food.getId(), food.getName(), food.getCategory(), food.getImageUrl(), recipes.size(), recipes);
    }

    private FoodDTO mapToSummaryDto(Food food, int recipeCount) {
        return new FoodDTO(food.getId(), food.getName(), food.getCategory(), food.getImageUrl(), recipeCount, List.of());
    }

    private Map<Long, Integer> getRecipeCountByFoodIds(List<Long> foodIds) {
        Map<Long, Integer> countByFoodId = new HashMap<>();
        for (var row : recipeRepository.countByFoodIds(foodIds)) {
            countByFoodId.put(row.getFoodId(), (int) row.getRecipeCount());
        }
        return countByFoodId;
    }

    private void createRecipeVersions(Long foodId, List<RecipeDTO> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        for (RecipeDTO recipe : recipes) {
            if (recipe == null) {
                throw new BusinessException("Recipe payload in food request must not be null");
            }
            RecipeDTO recipePayload = RecipeDTO.builder()
                    .version(recipe.getVersion())
                    .description(recipe.getDescription())
                    .ingredients(recipe.getIngredients())
                    .instructions(recipe.getInstructions())
                    .foodId(foodId)
                    .build();
            recipeService.createRecipe(recipePayload);
        }
    }
}
