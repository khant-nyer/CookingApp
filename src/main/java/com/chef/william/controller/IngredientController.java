package com.chef.william.controller;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Operation(summary = "Create one ingredient", description = "Accepts a single IngredientDTO JSON object")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingredient created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<IngredientDTO> createIngredient(@Valid @RequestBody IngredientDTO dto) {
        IngredientDTO created = ingredientService.createIngredient(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // CREATE BULK: POST /api/ingredients/bulk
    @Operation(summary = "Create ingredients in bulk", description = "Accepts a JSON array of IngredientDTO objects. Transaction is all-or-nothing.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingredients created"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate names")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<IngredientDTO>> createIngredientsBulk(
            @NotEmpty @RequestBody List<@Valid IngredientDTO> dtos) {
        List<IngredientDTO> created = ingredientService.createIngredients(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // READ: GET /api/ingredients/{id}
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<IngredientDTO> getIngredientById(@PathVariable Long id) {
        IngredientDTO dto = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(dto);
    }

    // READ: GET /api/ingredients (all)
    @GetMapping
    public ResponseEntity<Page<IngredientDTO>> getAllIngredients(Pageable pageable) {
        return ResponseEntity.ok(ingredientService.getAllIngredients(pageable));
    }

    // UPDATE: PUT /api/ingredients/{id}
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<IngredientDTO> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody IngredientDTO dto) {
        IngredientDTO updated = ingredientService.updateIngredient(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE: DELETE /api/ingredients/{id}
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.noContent().build();  // 204 No Content on success
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
