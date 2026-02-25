package com.chef.william.service.discovery;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
@RequiredArgsConstructor
public class CompositeSupermarketDiscoveryCrawler implements SupermarketDiscoveryCrawler {

    private final JsoupDuckDuckGoSupermarketDiscoveryCrawler duckDuckGoCrawler;
    private final FallbackSeedSupermarketDiscoveryCrawler fallbackSeedCrawler;

    @Override
    public List<DiscoveredSupermarket> discover(String city, String countryCode) {
        List<DiscoveredSupermarket> primary = duckDuckGoCrawler.discover(city, countryCode);
        if (!primary.isEmpty()) {
            return primary;
        }

        List<DiscoveredSupermarket> fallback = fallbackSeedCrawler.discover(city, countryCode);
        return deduplicateByHomepage(fallback);
    }

    private List<DiscoveredSupermarket> deduplicateByHomepage(List<DiscoveredSupermarket> source) {
        Map<String, DiscoveredSupermarket> byUrl = new LinkedHashMap<>();
        for (DiscoveredSupermarket market : source) {
            byUrl.putIfAbsent(market.homepage(), market);
        }
        return new ArrayList<>(byUrl.values());
    }
}
