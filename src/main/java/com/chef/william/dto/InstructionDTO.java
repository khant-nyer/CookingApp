package com.chef.william.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructionDTO {

    private Long id;  // null on create, populated on response

    @Min(value = 1, message = "Step must be at least 1")
    private Integer step;

    @NotBlank(message = "Instruction description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @URL(message = "Invalid video URL format")
    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String tutorialVideoUrl;
}