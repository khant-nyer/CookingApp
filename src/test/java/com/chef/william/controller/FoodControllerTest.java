package com.chef.william.controller;

import com.chef.william.dto.FoodDTO;
import com.chef.william.service.FoodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FoodControllerTest {

    @Mock
    private FoodService foodService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FoodController(foodService)).build();
    }

    @Test
    void createFoodShouldStillWorkWithoutRecipes() throws Exception {
        FoodDTO response = new FoodDTO(1L, "Tom Yum", "Soup", "https://img.example/tom-yum.jpg",
                null, null, null, 0, List.of());
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
