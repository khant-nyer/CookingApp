package com.chef.william.controller;

import com.chef.william.dto.FoodDTO;
import com.chef.william.service.FoodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FoodController.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unused")
    @MockBean
    private FoodService foodService;

    @Test
    void createFoodShouldStillWorkWithoutRecipes() throws Exception {
        FoodDTO response = new FoodDTO(1L, "Tom Yum", "Soup", "https://img.example/tom-yum.jpg", 0, List.of());
        when(foodService.createFood(any(FoodDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tom Yum",
                                  "category": "Soup"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tom Yum"))
                .andExpect(jsonPath("$.recipeCount").value(0))
                .andExpect(jsonPath("$.imageUrl").value("https://img.example/tom-yum.jpg"));
    }
}
