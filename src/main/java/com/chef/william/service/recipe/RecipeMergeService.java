package com.chef.william.service.recipe;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.Ingredient;
import com.chef.william.model.Instruction;
import com.chef.william.model.Recipe;
import com.chef.william.model.RecipeIngredient;
import com.chef.william.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecipeMergeService {

    private final IngredientRepository ingredientRepository;

    public void mergeIngredients(Recipe recipe, RecipeDTO dto) {
        Set<Long> incomingIngIds = new HashSet<>();
        for (RecipeIngredientDTO riDto : dto.getIngredients()) {
            if (!incomingIngIds.add(riDto.getIngredientId())) {
                throw new BusinessException("Duplicate ingredientId " + riDto.getIngredientId() + " in request");
            }
        }

        Map<Long, RecipeIngredient> existingMap = recipe.getRecipeIngredients().stream()
                .collect(Collectors.toMap(ri -> ri.getIngredient().getId(), ri -> ri));

        recipe.getRecipeIngredients().removeIf(ri -> !incomingIngIds.contains(ri.getIngredient().getId()));

        Set<Long> idsToFetch = dto.getIngredients().stream()
                .map(RecipeIngredientDTO::getIngredientId)
                .filter(id -> !existingMap.containsKey(id))
                .collect(Collectors.toSet());

        Map<Long, Ingredient> ingredientsById = ingredientRepository.findAllById(idsToFetch).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        if (ingredientsById.size() != idsToFetch.size()) {
            List<Long> missingIds = idsToFetch.stream()
                    .filter(id -> !ingredientsById.containsKey(id))
                    .toList();
            throw new ResourceNotFoundException("Ingredient not found with id(s): " + missingIds);
        }

        for (RecipeIngredientDTO riDto : dto.getIngredients()) {
            RecipeIngredient ri = existingMap.get(riDto.getIngredientId());

            if (ri != null) {
                ri.setQuantity(riDto.getQuantity());
                ri.setUnit(riDto.getUnit());
                ri.setNote(riDto.getNote());
                continue;
            }

            Ingredient ingredient = ingredientsById.get(riDto.getIngredientId());

            ri = new RecipeIngredient();
            ri.setRecipe(recipe);
            ri.setIngredient(ingredient);
            ri.setQuantity(riDto.getQuantity());
            ri.setUnit(riDto.getUnit());
            ri.setNote(riDto.getNote());
            recipe.getRecipeIngredients().add(ri);
        }
    }

    public void mergeInstructions(Recipe recipe, RecipeDTO dto) {
        Set<Integer> incomingSteps = new HashSet<>();
        for (InstructionDTO iDto : dto.getInstructions()) {
            if (!incomingSteps.add(iDto.getStep())) {
                throw new BusinessException("Duplicate step " + iDto.getStep() + " in request");
            }
        }

        Map<Integer, Instruction> existingMap = recipe.getInstructions().stream()
                .collect(Collectors.toMap(Instruction::getStep, i -> i));

        recipe.getInstructions().removeIf(ins -> !incomingSteps.contains(ins.getStep()));

        for (InstructionDTO iDto : dto.getInstructions()) {
            Instruction ins = existingMap.get(iDto.getStep());
            if (ins != null) {
                ins.setDescription(iDto.getDescription());
                ins.setTutorialVideoUrl(iDto.getTutorialVideoUrl());
                continue;
            }

            ins = new Instruction();
            ins.setRecipe(recipe);
            ins.setStep(iDto.getStep());
            ins.setDescription(iDto.getDescription());
            ins.setTutorialVideoUrl(iDto.getTutorialVideoUrl());
            recipe.getInstructions().add(ins);
        }
    }
}
