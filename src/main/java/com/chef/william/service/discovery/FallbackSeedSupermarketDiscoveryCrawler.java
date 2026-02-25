package com.chef.william.service.discovery;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FallbackSeedSupermarketDiscoveryCrawler implements SupermarketDiscoveryCrawler {

    private static final Map<String, List<DiscoveredSupermarket>> COUNTRY_SEEDS = Map.of(
            "ID", List.of(
                    new DiscoveredSupermarket("Tokopedia Fresh", "https://www.tokopedia.com", "seed", "LOW"),
                    new DiscoveredSupermarket("Alfamidi", "https://alfagift.id", "seed", "LOW")
            ),
            "US", List.of(
                    new DiscoveredSupermarket("Walmart", "https://www.walmart.com", "seed", "LOW"),
                    new DiscoveredSupermarket("Target", "https://www.target.com", "seed", "LOW")
            ),
            "GB", List.of(
                    new DiscoveredSupermarket("Tesco", "https://www.tesco.com", "seed", "LOW"),
                    new DiscoveredSupermarket("Sainsbury's", "https://www.sainsburys.co.uk", "seed", "LOW")
            )
    );

    @Override
    public List<DiscoveredSupermarket> discover(String city, String countryCode) {
        return COUNTRY_SEEDS.getOrDefault(countryCode, List.of());
    }
}
