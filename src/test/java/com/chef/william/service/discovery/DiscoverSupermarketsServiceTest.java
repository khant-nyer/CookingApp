package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoverSupermarketsServiceTest {

    private final SupermarketMatchingService matchingService = new DefaultSupermarketMatchingService();

    @Test
    void shouldReturnSuccessWhenInspectionCompletedAndMatchingFound() {
        SupermarketDiscoveryCrawler crawler = (city, countryCode) -> List.of(
                new DiscoveredSupermarket("Fresh Mart", "https://freshmart.example", "duckduckgo_html", "LOW"),
                new DiscoveredSupermarket("No Match Store", "https://nomatch.example", "duckduckgo_html", "LOW")
        );
        SupermarketInspectionService inspector = (market, ingredient) -> {
            if (market.name().contains("Fresh")) {
                return new SupermarketInspectionResult(
                        market.name(),
                        market.homepage(),
                        market.homepage() + "/search?q=" + ingredient,
                        true,
                        "MEDIUM",
                        true
                );
            }

            return new SupermarketInspectionResult(
                    market.name(),
                    market.homepage(),
                    market.homepage() + "/search?q=" + ingredient,
                    false,
                    "LOW",
                    true
            );
        };

        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                crawler,
                inspector,
                matchingService
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.meta().resolvedCountryCode()).isEqualTo("ID");
        assertThat(response.meta().phase()).isEqualTo("phase3");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().name()).isEqualTo("Fresh Mart");
        assertThat(response.data().getFirst().available()).isTrue();
    }

    @Test
    void shouldReturnFallbackWhenCountryNotDetected() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of(),
                (market, ingredient) -> new SupermarketInspectionResult("", "", "", false, "LOW", false),
                matchingService
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
                (market, ingredient) -> new SupermarketInspectionResult("", "", "", false, "LOW", false),
                matchingService
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
                ),
                matchingService
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase2");
        assertThat(response.message()).contains("couldn’t inspect supermarket results");
    }

    @Test
    void shouldReturnFallbackWhenNoStrongMatchAfterInspection() {
        DiscoverSupermarketsService service = new DiscoverSupermarketsService(
                new CountryDetectionService(),
                (city, countryCode) -> List.of(new DiscoveredSupermarket("Fresh Mart", "https://freshmart.example", "duckduckgo_html", "LOW")),
                (market, ingredient) -> new SupermarketInspectionResult(
                        market.name(), market.homepage(), market.homepage() + "/search?q=" + ingredient, false, "LOW", true
                ),
                matchingService
        );

        DiscoverSupermarketsResponse response = service.discoverSupermarkets(
                new DiscoverSupermarketsRequest("milk", "Jakarta")
        );

        assertThat(response.status()).isEqualTo("fallback");
        assertThat(response.meta().phase()).isEqualTo("phase3");
        assertThat(response.message()).contains("No supermarkets currently show strong matches");
    }
}
