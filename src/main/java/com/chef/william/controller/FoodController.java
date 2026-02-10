package com.chef.william.controller;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.service.FoodService;
import com.chef.william.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<FoodDTO> create(@Valid @RequestBody FoodDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(foodService.createFood(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getFoodById(id));
    }

    @GetMapping
    public ResponseEntity<List<FoodDTO>> getAll() {
        return ResponseEntity.ok(foodService.getAllFoods());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodDTO> update(@PathVariable Long id, @Valid @RequestBody FoodDTO dto) {
        return ResponseEntity.ok(foodService.updateFood(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        foodService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recipe-status")
    public ResponseEntity<FoodRecipeStatusDTO> recipeStatus(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getFoodRecipeStatus(id));
    }

    @PostMapping("/{id}/recipes")
    public ResponseEntity<RecipeDTO> createRecipeForFood(@PathVariable Long id,
                                                          @Valid @RequestBody RecipeDTO recipeDTO) {
        recipeDTO.setFoodId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.createRecipe(recipeDTO));
    }
}
