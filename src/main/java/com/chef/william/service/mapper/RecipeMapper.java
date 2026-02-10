package com.chef.william.service.mapper;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.model.Recipe;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RecipeMapper {

    public RecipeDTO toDto(Recipe recipe) {
        RecipeDTO dto = RecipeDTO.builder()
                .id(recipe.getId())
                .version(recipe.getVersion())
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
