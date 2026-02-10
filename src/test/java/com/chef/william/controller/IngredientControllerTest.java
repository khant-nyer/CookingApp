package com.chef.william.controller;

import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.service.IngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngredientService ingredientService;

    @Test
    void discoverSupermarketsEndpointShouldReturnMatches() throws Exception {
        when(ingredientService.discoverPopularSupermarkets(1L, "Bangkok", "tomato"))
                .thenReturn(List.of(new SupermarketDiscoveryDTO(
                        "Bangkok",
                        "Big C",
                        "https://bigc.example",
                        "https://bigc.example/search?q=tomato",
                        true,
                        "OFFICIAL_WEB_CRAWL",
                        LocalDateTime.now()
                )));

        mockMvc.perform(get("/api/ingredients/discover-supermarkets")
                        .param("ingredientName", "tomato")
                        .param("city", "Bangkok")
                        .param("userId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supermarketName").value("Big C"))
                .andExpect(jsonPath("$[0].ingredientMatched").value(true));
    }

    @Test
    void discoverSupermarketsRouteMustNotBeCapturedByIdRoute() throws Exception {
        when(ingredientService.discoverPopularSupermarkets(null, "Bangkok", "onion"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/ingredients/discover-supermarkets")
                        .param("ingredientName", "onion")
                        .param("city", "Bangkok")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void discoverSupermarketsPathVariantShouldSupportEncodedIngredientName() throws Exception {
        when(ingredientService.discoverPopularSupermarkets(null, "Bangkok", "Soy Sauce"))
                .thenReturn(List.of(new SupermarketDiscoveryDTO(
                        "Bangkok",
                        "Lotus",
                        "https://lotus.example",
                        "https://lotus.example/search?q=soy+sauce",
                        true,
                        "OFFICIAL_WEB_CRAWL",
                        LocalDateTime.now()
                )));

        mockMvc.perform(get("/api/ingredients/Soy%20Sauce/discover-supermarkets")
                        .param("city", "Bangkok")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supermarketName").value("Lotus"));
    }


}
