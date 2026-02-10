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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;

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
    public List<FoodDTO> getAllFoods() {
        return foodRepository.findAll().stream().map(this::mapToDto).toList();
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

        int recipeCount = food.getRecipes() == null ? 0 : food.getRecipes().size();
        boolean hasRecipe = recipeCount > 0;
        String message = hasRecipe
                ? "Recipes are available for this food"
                : "There is no recipe for this food yet, create one";

        return new FoodRecipeStatusDTO(food.getId(), food.getName(), hasRecipe, message);
    }

    private void mapToEntity(FoodDTO dto, Food entity) {
        entity.setName(normalizeName(dto.getName()));
        entity.setCategory(dto.getCategory());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private FoodDTO mapToDto(Food food) {
        int recipeCount = recipeRepository.countByFoodId(food.getId());
        return new FoodDTO(food.getId(), food.getName(), food.getCategory(), recipeCount, List.of());
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
