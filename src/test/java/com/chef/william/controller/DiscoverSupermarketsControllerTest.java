package com.chef.william.controller;

import com.chef.william.dto.discovery.DiscoverSupermarketsMeta;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import com.chef.william.service.discovery.DiscoverSupermarketsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(DiscoverSupermarketsController.class)
class DiscoverSupermarketsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscoverSupermarketsService discoverSupermarketsService;

    @Test
    void shouldReturnPhase0Success() throws Exception {
        when(discoverSupermarketsService.discoverSupermarkets(any())).thenReturn(
                new DiscoverSupermarketsResponse(
                        "success",
                        "Phase 0 completed: country detected. Discovery crawl starts in Phase 1.",
                        List.of(),
                        new DiscoverSupermarketsMeta("Jakarta", "ID", "MEDIUM", "phase0")
                )
        );

        mockMvc.perform(post("/api/supermarkets/discover-supermarkets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ingredient": "milk",
                              "city": "Jakarta"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.meta.resolvedCountryCode").value("ID"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/supermarkets/discover-supermarkets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "ingredient": "",
                              "city": ""
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Failed"));
    }
}
