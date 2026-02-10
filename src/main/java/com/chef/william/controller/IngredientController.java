package com.chef.william.controller;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.IngredientStoreListingDTO;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // CREATE: POST /api/ingredients
    @PostMapping
    public ResponseEntity<IngredientDTO> createIngredient(@Valid @RequestBody IngredientDTO dto) {
        IngredientDTO created = ingredientService.createIngredient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // READ: GET /api/ingredients/{id}
    @GetMapping("/{id}")
    public ResponseEntity<IngredientDTO> getIngredientById(@PathVariable Long id) {
        IngredientDTO dto = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(dto);
    }


    @GetMapping("/{id}/store-locations")
    public ResponseEntity<List<IngredientStoreListingDTO>> getIngredientStoreLocations(@PathVariable Long id) {
        List<IngredientStoreListingDTO> locations = ingredientService.getIngredientStoreLocations(id);
        return ResponseEntity.ok(locations);
    }

    // READ: GET /api/ingredients (all)
    @GetMapping
    public ResponseEntity<List<IngredientDTO>> getAllIngredients() {
        List<IngredientDTO> ingredients = ingredientService.getAllIngredients();
        return ResponseEntity.ok(ingredients);
    }

    // UPDATE: PUT /api/ingredients/{id}
    @PutMapping("/{id}")
    public ResponseEntity<IngredientDTO> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody IngredientDTO dto) {
        IngredientDTO updated = ingredientService.updateIngredient(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE: DELETE /api/ingredients/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.noContent().build();  // 204 No Content on success
    }


    @GetMapping("/discover-supermarkets")
    public ResponseEntity<List<SupermarketDiscoveryDTO>> discoverSupermarkets(
            @RequestParam(name = "ingredientName") String ingredientName,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "userId", required = false) Long userId) {
        List<SupermarketDiscoveryDTO> results = ingredientService
                .discoverPopularSupermarkets(userId, city, ingredientName);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<List<IngredientDTO>> searchIngredients(
            @RequestParam(name = "name", required = false) String name) {
        List<IngredientDTO> results = ingredientService.searchIngredientsByName(name);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/by-nutrition")
    public ResponseEntity<List<IngredientDTO>> searchIngredientByNutrient(
            @RequestParam("nutrient") String nutrient,
            @RequestParam(value = "minValue", required = false) Double minValue) {

        List<IngredientDTO> results = ingredientService.searchIngredientByNutrient(nutrient, minValue);
        return ResponseEntity.ok(results);
    }
}