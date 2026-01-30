package com.chef.william.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeDTO {

    private Long id;  // null on create, populated on response

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotEmpty(message = "At least one ingredient is required")
    @Valid
    @Builder.Default
    private List<RecipeIngredientDTO> ingredients = new ArrayList<>();

    @NotEmpty(message = "At least one instruction is required")
    @Valid
    @Builder.Default
    private List<InstructionDTO> instructions = new ArrayList<>();

    //private UserDTO author;

}