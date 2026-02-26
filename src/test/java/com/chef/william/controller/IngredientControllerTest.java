package com.chef.william.controller;

import com.chef.william.dto.IngredientDTO;
import com.chef.william.dto.discovery.SupermarketDTO;
import com.chef.william.dto.discovery.SupermarketDiscoveryResponseDTO;
import com.chef.william.model.enums.Unit;
import com.chef.william.service.IngredientService;
import com.chef.william.service.ingredient.discovery.SupermarketDiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private IngredientService ingredientService;

    @MockBean
    private SupermarketDiscoveryService supermarketDiscoveryService;


    @Test
    void createIngredientsBulkEndpointShouldAcceptArrayPayload() throws Exception {
        IngredientDTO ingredient = new IngredientDTO();
        ingredient.setId(1L);
        ingredient.setName("Mascarpone Cheese");
        ingredient.setServingAmount(100.0);
        ingredient.setServingUnit(Unit.G);

        when(ingredientService.createIngredients(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(ingredient));

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


    @Test
    void discoverSupermarketsShouldReturnDiscoveredList() throws Exception {
        SupermarketDiscoveryResponseDTO response = SupermarketDiscoveryResponseDTO.builder()
                .ingredientName("tomato")
                .city("Bangkok")
                .country("Thailand")
                .fallbackUsed(false)
                .supermarkets(List.of(
                        SupermarketDTO.builder()
                                .name("Big C")
                                .officialOnlineWebpage("https://www.bigc.co.th")
                                .matchedIngredientPriceRange("25.00-45.00")
                                .address("Rajdamri Road, Bangkok")
                                .city("Bangkok")
                                .country("Thailand")
                                .source("OPENSTREETMAP")
                                .build()
                ))
                .build();

        when(supermarketDiscoveryService.discover("tomato", "Bangkok"))
                .thenReturn(response);

        mockMvc.perform(get("/api/ingredients/discover-supermarkets")
                        .param("ingredientName", "tomato")
                        .param("city", "Bangkok")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Bangkok"))
                .andExpect(jsonPath("$.country").value("Thailand"))
                .andExpect(jsonPath("$.supermarkets[0].name").value("Big C"))
                .andExpect(jsonPath("$.supermarkets[0].officialOnlineWebpage").value("https://www.bigc.co.th"))
                .andExpect(jsonPath("$.supermarkets[0].matchedIngredientPriceRange").value("25.00-45.00"))
                .andExpect(jsonPath("$.supermarkets[0].address").value("Rajdamri Road, Bangkok"));
    }

}
