package com.chef.william.service.ingredient;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.chef.william.dto.discovery.SupermarketDiscoveryResponseDTO;
import com.chef.william.service.ingredient.discovery.OpenStreetMapSupermarketDiscoveryClient;
import com.chef.william.service.ingredient.discovery.PlaywrightSupermarketDiscoveryClient;
import com.chef.william.service.ingredient.discovery.SupermarketDiscoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupermarketDiscoveryServiceTest {

    @Mock
    private OpenStreetMapSupermarketDiscoveryClient openStreetMapClient;

    @Mock
    private PlaywrightSupermarketDiscoveryClient playwrightClient;

    @InjectMocks
    private SupermarketDiscoveryService supermarketDiscoveryService;

    @Test
    void shouldUseApiResultsBeforeFallback() {
        when(openStreetMapClient.resolveCity("Bangkok"))
                .thenReturn(Optional.of(new OpenStreetMapSupermarketDiscoveryClient.CityContext("Bangkok", "Thailand", "th", "13.7563", "100.5018")));
        when(openStreetMapClient.discoverSupermarkets("Bangkok", "Thailand", any()))
                .thenReturn(List.of(SupermarketDTO.builder().name("Big C").source("OPENSTREETMAP").build()));

        SupermarketDiscoveryResponseDTO response = supermarketDiscoveryService.discover("tomato", "Bangkok");

        assertThat(response.isFallbackUsed()).isFalse();
        assertThat(response.getSupermarkets()).hasSize(1);
        assertThat(response.getSupermarkets().getFirst().getName()).isEqualTo("Big C");
    }

    @Test
    void shouldUsePlaywrightFallbackWhenApiReturnsNone() {
        when(openStreetMapClient.resolveCity("Bangkok"))
                .thenReturn(Optional.of(new OpenStreetMapSupermarketDiscoveryClient.CityContext("Bangkok", "Thailand", "th", "13.7563", "100.5018")));
        when(openStreetMapClient.discoverSupermarkets("Bangkok", "Thailand", any()))
                .thenReturn(List.of());
        when(playwrightClient.discoverBySearch("Bangkok", "Thailand"))
                .thenReturn(List.of(SupermarketDTO.builder().name("Lotus's").source("PLAYWRIGHT_FALLBACK").build()));

        SupermarketDiscoveryResponseDTO response = supermarketDiscoveryService.discover("tomato", "Bangkok");

        assertThat(response.isFallbackUsed()).isTrue();
        assertThat(response.getSupermarkets()).hasSize(1);
        assertThat(response.getSupermarkets().getFirst().getSource()).isEqualTo("PLAYWRIGHT_FALLBACK");
    }
}
