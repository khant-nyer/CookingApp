package com.chef.william.controller;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.FoodRecipeStatusDTO;
import com.chef.william.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @PostMapping
    public ResponseEntity<FoodDTO> create(@Valid @RequestBody FoodDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(foodService.createFood(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getFoodById(id));
    }

    @GetMapping
    public ResponseEntity<Page<FoodDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(foodService.getAllFoods(pageable));
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
}
