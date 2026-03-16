package com.chef.william.controller;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.model.enums.Unit;
import com.chef.william.service.IngredientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IngredientControllerTest {

    @Mock
    private IngredientService ingredientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new IngredientController(ingredientService)).build();
    }

    @Test
    void createIngredientsBulkEndpointShouldAcceptArrayPayload() throws Exception {
        IngredientDTO ingredient = new IngredientDTO();
        ingredient.setId(1L);
        ingredient.setName("Mascarpone Cheese");
        ingredient.setServingAmount(100.0);
        ingredient.setServingUnit(Unit.G);

        when(ingredientService.createIngredients(anyList())).thenReturn(List.of(ingredient));

        String payload = """
                [
                  {
                    "name": "Mascarpone Cheese",
                    "category": "Dairy",
                    "description": "Cream cheese for tiramisu",
                    "servingAmount": 100.0,
                    "servingUnit": "G",
                    "nutrients": [
                      { "nutrient": "CALORIES", "value": 429.0, "unit": "kcal" }
                    ]
                  }
                ]
                """;

        mockMvc.perform(post("/api/ingredients/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Mascarpone Cheese"));
    }
}
