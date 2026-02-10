package com.chef.william.service;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    @Transactional
    public FoodDTO createFood(FoodDTO dto) {
        Food food = new Food();
        mapToEntity(dto, food);
        return mapToDto(foodRepository.save(food));
    }

    @Transactional
    public FoodDTO updateFood(Long id, FoodDTO dto) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + id));
        mapToEntity(dto, food);
        return mapToDto(foodRepository.save(food));
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
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
    }

    private FoodDTO mapToDto(Food food) {
        int recipeCount = food.getRecipes() == null ? 0 : food.getRecipes().size();
        return new FoodDTO(food.getId(), food.getName(), food.getCategory(), recipeCount);
    }
}
