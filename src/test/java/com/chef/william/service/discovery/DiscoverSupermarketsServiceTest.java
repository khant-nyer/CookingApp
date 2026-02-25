package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverSupermarketsServiceTest {

    @Test
    void shouldReturnSuccessWhenCountryDetectedAndMarketsFound() {
        SupermarketDiscoveryCrawler crawler = (city, countryCode) -> List.of(
                new DiscoveredSupermarket("Fresh Mart", "https://freshmart.example", "duckduckgo_html", "LOW")
        );

        DiscoverSupermarketsService service = new DiscoverSupermarketsService(new CountryDetectionService(), crawler);

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.meta().phase()).isEqualTo("phase1");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().name()).isEqualTo("Fresh Mart");
    }

    @Test
    void shouldReturnFallbackWhenCountryNotDetected() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of()
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Springfield")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase0");
        assertThat(response.meta().resolvedCountryCode()).isNull();
        assertThat(response.message()).contains("couldn’t confidently detect the country");
    }

    @Test
    void shouldReturnFallbackWhenNoMarketDiscovered() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of()
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase1");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.message()).contains("No online supermarkets were discovered");
    }
}
