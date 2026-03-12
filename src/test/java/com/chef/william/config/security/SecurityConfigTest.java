package com.chef.william.config.security;

import com.chef.william.controller.RecipeController;
import com.chef.william.dto.RecipeDTO;
import com.chef.william.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void protectedEndpointShouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void preflightRequestShouldBeAllowedWithoutToken() throws Exception {
        mockMvc.perform(options("/api/recipes/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointShouldAllowRequestWhenJwtPresent() throws Exception {
        when(recipeService.getRecipeById(anyLong())).thenReturn(RecipeDTO.builder().id(1L).version("v1").build());

        Jwt token = new Jwt(
                "token",
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "abc", "token_use", "access", "client_id", "2v36scmicr5rqoqio57g4hnqcv")
        );

        mockMvc.perform(get("/api/recipes/1").with(jwt().jwt(token)))
                .andExpect(status().isOk());
    }
}
