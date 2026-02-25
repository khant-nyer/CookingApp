package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverSupermarketsServiceTest {

    @Test
    void shouldReturnSuccessWhenInspectionCompleted() {
        SupermarketDiscoveryCrawler crawler = (city, countryCode) -> List.of(
                new DiscoveredSupermarket("Fresh Mart", "https://freshmart.example", "duckduckgo_html", "LOW")
        );
        SupermarketInspectionService inspector = (market, ingredient) -> new SupermarketInspectionResult(
                market.name(),
                market.homepage(),
                market.homepage() + "/search?q=" + ingredient,
                true,
                "MEDIUM",
                true
        );

        DiscoverSupermarketsService service = new DiscoverSupermarketsService(new CountryDetectionService(), crawler, inspector);

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.meta().phase()).isEqualTo("phase2");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().available()).isTrue();
        assertThat(response.data().getFirst().ingredientSearchUrl()).contains("search?q=milk");
    }

    @Test
    void shouldReturnFallbackWhenCountryNotDetected() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of(),
                (market, ingredient) -> new SupermarketInspectionResult("", "", "", false, "LOW", false)
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
                (city, countryCode) -> List.of(),
                (market, ingredient) -> new SupermarketInspectionResult("", "", "", false, "LOW", false)
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase1");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.message()).contains("No online supermarkets were discovered");
    }

    @Test
    void shouldReturnFallbackWhenInspectionFailsForAllMarkets() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of(new DiscoveredSupermarket("Fresh Mart", "https://freshmart.example", "duckduckgo_html", "LOW")),
                (market, ingredient) -> new SupermarketInspectionResult(
                        market.name(), market.homepage(), market.homepage(), false, "LOW", false
                )
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase2");
        assertThat(response.message()).contains("couldn’t inspect supermarket results");
    }
}
