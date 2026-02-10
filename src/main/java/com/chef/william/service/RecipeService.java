package com.chef.william.service;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Recipe;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.RecipeRepository;
import com.chef.william.service.mapper.RecipeMapper;
import com.chef.william.service.recipe.RecipeMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final FoodRepository foodRepository;
    private final RecipeMergeService recipeMergeService;
    private final RecipeMapper recipeMapper;

    @Transactional
    public RecipeDTO createRecipe(RecipeDTO recipeDTO) {
        Recipe recipe = new Recipe();
        populateScalars(recipe, recipeDTO);
        recipeMergeService.mergeIngredients(recipe, recipeDTO);
        recipeMergeService.mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return recipeMapper.toDto(recipe);
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, RecipeDTO recipeDTO) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        populateScalars(recipe, recipeDTO);
        recipeMergeService.mergeIngredients(recipe, recipeDTO);
        recipeMergeService.mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return recipeMapper.toDto(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeDTO getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        return recipeMapper.toDto(recipe);
    }

    @Transactional(readOnly = true)
    public List<RecipeDTO> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(recipeMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recipe not found with id: " + id);
        }
        recipeRepository.deleteById(id);
    }

    private void populateScalars(Recipe recipe, RecipeDTO dto) {
        recipe.setVersion(dto.getVersion());
        recipe.setDescription(dto.getDescription());

        if (dto.getFoodId() != null) {
            Food food = foodRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + dto.getFoodId()));
            recipe.setFood(food);
        } else {
            recipe.setFood(null);
        }
    }
}
