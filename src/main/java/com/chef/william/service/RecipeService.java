package com.chef.william.service;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Food;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Instruction;
import com.chef.william.model.Recipe;
import com.chef.william.model.RecipeIngredient;
import com.chef.william.repository.FoodRepository;
import com.chef.william.repository.IngredientRepository;
import com.chef.william.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final FoodRepository foodRepository;

    @Transactional
    public RecipeDTO createRecipe(RecipeDTO recipeDTO) {
        Recipe recipe = new Recipe();
        populateScalars(recipe, recipeDTO);
        mergeIngredients(recipe, recipeDTO);
        mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return mapToDTO(recipe);
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, RecipeDTO recipeDTO) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        populateScalars(recipe, recipeDTO);
        mergeIngredients(recipe, recipeDTO);
        mergeInstructions(recipe, recipeDTO);
        recipe = recipeRepository.save(recipe);
        return mapToDTO(recipe);
    }

    @Transactional(readOnly = true)
    public RecipeDTO getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        return mapToDTO(recipe);
    }

    @Transactional(readOnly = true)
    public List<RecipeDTO> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(this::mapToDTO)
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
        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());

        if (dto.getFoodId() != null) {
            Food food = foodRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + dto.getFoodId()));
            recipe.setFood(food);
        } else {
            recipe.setFood(null);
        }
    }

    private void mergeIngredients(Recipe recipe, RecipeDTO dto) {
        // Validate no duplicate ingredientIds in incoming DTO
        Set<Long> incomingIngIds = new HashSet<>();
        for (RecipeIngredientDTO riDto : dto.getIngredients()) {
            if (!incomingIngIds.add(riDto.getIngredientId())) {
                throw new BusinessException("Duplicate ingredientId " + riDto.getIngredientId() + " in request");
            }
        }

        // Map existing by ingredientId
        Map<Long, RecipeIngredient> existingMap = recipe.getRecipeIngredients().stream()
                .collect(Collectors.toMap(ri -> ri.getIngredient().getId(), ri -> ri));

        // Remove existing not in incoming
        recipe.getRecipeIngredients().removeIf(ri -> !incomingIngIds.contains(ri.getIngredient().getId()));

        // Merge
        for (RecipeIngredientDTO riDto : dto.getIngredients()) {
            RecipeIngredient ri = existingMap.get(riDto.getIngredientId());

            if (ri != null) {
                // Update existing (ingredient cannot change)
                ri.setQuantity(riDto.getQuantity());
                ri.setUnit(riDto.getUnit());
                ri.setNote(riDto.getNote());
            } else {
                // Add new
                Ingredient ingredient = ingredientRepository.findById(riDto.getIngredientId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Ingredient not found with id: " + riDto.getIngredientId()));

                ri = new RecipeIngredient();
                ri.setRecipe(recipe);
                ri.setIngredient(ingredient);
                ri.setQuantity(riDto.getQuantity());
                ri.setUnit(riDto.getUnit());
                ri.setNote(riDto.getNote());
                recipe.getRecipeIngredients().add(ri);
            }
        }
    }

    private void mergeInstructions(Recipe recipe, RecipeDTO dto) {
        // Validate no duplicate steps in incoming DTO
        Set<Integer> incomingSteps = new HashSet<>();
        for (InstructionDTO iDto : dto.getInstructions()) {
            if (!incomingSteps.add(iDto.getStep())) {
                throw new BusinessException("Duplicate step " + iDto.getStep() + " in request");
            }
        }

        // Map existing by step
        Map<Integer, Instruction> existingMap = recipe.getInstructions().stream()
                .collect(Collectors.toMap(Instruction::getStep, i -> i));

        // Remove existing not in incoming
        recipe.getInstructions().removeIf(ins -> !incomingSteps.contains(ins.getStep()));

        // Merge
        for (InstructionDTO iDto : dto.getInstructions()) {
            Instruction ins = existingMap.get(iDto.getStep());

            if (ins != null) {
                // Update existing
                ins.setDescription(iDto.getDescription());
                ins.setTutorialVideoUrl(iDto.getTutorialVideoUrl());
                // step remains the same (immutable identifier)
            } else {
                // Add new
                ins = new Instruction();
                ins.setRecipe(recipe);
                ins.setStep(iDto.getStep());
                ins.setDescription(iDto.getDescription());
                ins.setTutorialVideoUrl(iDto.getTutorialVideoUrl());
                recipe.getInstructions().add(ins);
            }
        }
    }

    private RecipeDTO mapToDTO(Recipe recipe) {
        RecipeDTO dto = RecipeDTO.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .foodId(recipe.getFood() != null ? recipe.getFood().getId() : null)
                .foodName(recipe.getFood() != null ? recipe.getFood().getName() : null)
                .build();

        List<RecipeIngredientDTO> ingredientDTOs = recipe.getRecipeIngredients().stream()
                .map(ri -> new RecipeIngredientDTO(
                        ri.getId(),
                        ri.getIngredient().getId(),
                        ri.getIngredient().getName(),
                        ri.getQuantity(),
                        ri.getUnit(),
                        ri.getNote()
                ))
                .sorted(Comparator.comparing(RecipeIngredientDTO::getIngredientName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        dto.setIngredients(new ArrayList<>(ingredientDTOs));

        List<InstructionDTO> instructionDTOs = recipe.getInstructions().stream()
                .map(ins -> InstructionDTO.builder()
                        .id(ins.getId())
                        .step(ins.getStep())
                        .description(ins.getDescription())
                        .tutorialVideoUrl(ins.getTutorialVideoUrl())
                        .build())
                .sorted(Comparator.comparing(InstructionDTO::getStep,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        dto.setInstructions(new ArrayList<>(instructionDTOs));

        return dto;
    }
}