package com.chef.william.service.ingredient;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.chef.william.dto.discovery.SupermarketDiscoveryResponseDTO;
import com.chef.william.service.ingredient.discovery.OpenStreetMapSupermarketDiscoveryClient;
import com.chef.william.service.ingredient.discovery.SearchEngineSupermarketDiscoveryClient;
import com.chef.william.service.ingredient.discovery.SupermarketDiscoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupermarketDiscoveryServiceTest {

    @Mock
    private OpenStreetMapSupermarketDiscoveryClient openStreetMapClient;

    @Mock
    private SearchEngineSupermarketDiscoveryClient searchEngineClient;

    @InjectMocks
    private SupermarketDiscoveryService supermarketDiscoveryService;

    @Test
    void shouldUseApiResultsBeforeFallback() {
        when(openStreetMapClient.resolveCity("Bangkok"))
                .thenReturn(Optional.of(new OpenStreetMapSupermarketDiscoveryClient.CityContext("Bangkok", "Thailand", "th")));
        when(openStreetMapClient.discoverSupermarkets("Bangkok", "Thailand"))
                .thenReturn(List.of(SupermarketDTO.builder().name("Big C").source("OPENSTREETMAP").build()));

        SupermarketDiscoveryResponseDTO response = supermarketDiscoveryService.discover("tomato", "Bangkok");

        assertThat(response.isFallbackUsed()).isFalse();
        assertThat(response.getSupermarkets()).hasSize(1);
        assertThat(response.getSupermarkets().getFirst().getName()).isEqualTo("Big C");
    }

    @Test
    void shouldUseSearchFallbackWhenApiReturnsNone() {
        when(openStreetMapClient.resolveCity("Bangkok"))
                .thenReturn(Optional.of(new OpenStreetMapSupermarketDiscoveryClient.CityContext("Bangkok", "Thailand", "th")));
        when(openStreetMapClient.discoverSupermarkets("Bangkok", "Thailand"))
                .thenReturn(List.of());
        when(searchEngineClient.discoverBySearch("Bangkok", "Thailand"))
                .thenReturn(List.of(SupermarketDTO.builder().name("Lotus's").source("SEARCH_ENGINE_FALLBACK").build()));

        SupermarketDiscoveryResponseDTO response = supermarketDiscoveryService.discover("tomato", "Bangkok");

        assertThat(response.isFallbackUsed()).isTrue();
        assertThat(response.getSupermarkets()).hasSize(1);
        assertThat(response.getSupermarkets().getFirst().getSource()).isEqualTo("SEARCH_ENGINE_FALLBACK");
    }
}
