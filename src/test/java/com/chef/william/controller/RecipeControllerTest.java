package com.chef.william.controller;

import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.model.enums.Unit;
import com.chef.william.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecipeController(recipeService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createRecipeForFoodShouldUseFoodIdFromPath() throws Exception {
        RecipeDTO request = RecipeDTO.builder()
                .version("v1")
                .description("desc")
                .ingredients(List.of(new RecipeIngredientDTO(null, 1L, null, 1.0, Unit.G, null)))
                .instructions(List.of(InstructionDTO.builder().step(1).description("mix").build()))
                .foodId(999L)
                .build();

        RecipeDTO response = RecipeDTO.builder()
                .id(100L)
                .version("v1")
                .foodId(5L)
                .foodName("Pad Thai")
                .ingredients(List.of(new RecipeIngredientDTO(null, 1L, "Salt", 1.0, Unit.G, null)))
                .instructions(List.of(InstructionDTO.builder().step(1).description("mix").build()))
                .build();

        when(recipeService.createRecipe(any(RecipeDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/recipes/foods/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodId").value(5))
                .andExpect(jsonPath("$.version").value("v1"));

        ArgumentCaptor<RecipeDTO> captor = ArgumentCaptor.forClass(RecipeDTO.class);
        verify(recipeService).createRecipe(captor.capture());
        assertEquals(5L, captor.getValue().getFoodId());
    }
}
