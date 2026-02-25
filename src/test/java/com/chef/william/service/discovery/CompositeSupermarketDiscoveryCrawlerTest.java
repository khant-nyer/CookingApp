package com.chef.william.service.discovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeSupermarketDiscoveryCrawlerTest {

    @Test
    void shouldFallbackToSeedWhenPrimaryEmpty() {
        JsoupDuckDuckGoSupermarketDiscoveryCrawler primary = new JsoupDuckDuckGoSupermarketDiscoveryCrawler() {
            @Override
            public List<DiscoveredSupermarket> discover(String city, String countryCode) {
                return List.of();
            }
        };

        FallbackSeedSupermarketDiscoveryCrawler fallback = new FallbackSeedSupermarketDiscoveryCrawler();
        CompositeSupermarketDiscoveryCrawler composite = new CompositeSupermarketDiscoveryCrawler(primary, fallback);

        List<DiscoveredSupermarket> result = composite.discover("Jakarta", "ID");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().source()).isEqualTo("seed");
    }
}
