package com.chef.william.controller;

import com.chef.william.dto.FoodDTO;
import com.chef.william.dto.InstructionDTO;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.dto.RecipeIngredientDTO;
import com.chef.william.model.enums.Unit;
import com.chef.william.service.FoodService;
import com.chef.william.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FoodService foodService;

    @MockBean
    private RecipeService recipeService;

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

        mockMvc.perform(post("/api/foods/5/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodId").value(5))
                .andExpect(jsonPath("$.version").value("v1"));

        ArgumentCaptor<RecipeDTO> captor = ArgumentCaptor.forClass(RecipeDTO.class);
        verify(recipeService).createRecipe(captor.capture());
        assertEquals(5L, captor.getValue().getFoodId());
    }

    @Test
    void createFoodShouldStillWorkWithoutRecipes() throws Exception {
        FoodDTO response = new FoodDTO(1L, "Tom Yum", "Soup", 0, List.of());
        when(foodService.createFood(any(FoodDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"name\": \"Tom Yum\",
                                  \"category\": \"Soup\"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tom Yum"))
                .andExpect(jsonPath("$.recipeCount").value(0));
    }
}
