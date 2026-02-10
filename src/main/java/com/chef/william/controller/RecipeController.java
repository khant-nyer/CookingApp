package com.chef.william.controller;

import com.chef.william.dto.RecipeDTO;
import com.chef.william.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<RecipeDTO> create(@Valid @RequestBody RecipeDTO recipeDTO) {
        RecipeDTO created = recipeService.createRecipe(recipeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/foods/{foodId}")
    public ResponseEntity<RecipeDTO> createForFood(@PathVariable Long foodId,
                                                   @Valid @RequestBody RecipeDTO recipeDTO) {
        recipeDTO.setFoodId(foodId);
        RecipeDTO created = recipeService.createRecipe(recipeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getById(@PathVariable Long id) {
        RecipeDTO dto = recipeService.getRecipeById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDTO> update(@PathVariable Long id,
                                            @Valid @RequestBody RecipeDTO recipeDTO) {
        RecipeDTO updated = recipeService.updateRecipe(id, recipeDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RecipeDTO>> getAll() {
        List<RecipeDTO> recipes = recipeService.getAllRecipes();
        return ResponseEntity.ok(recipes);
    }
}
