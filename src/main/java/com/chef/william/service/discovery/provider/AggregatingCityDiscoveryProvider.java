package com.chef.william.service.discovery.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Primary
@RequiredArgsConstructor
public class AggregatingCityDiscoveryProvider implements CityDiscoveryProvider {

    private final List<CityDiscoverySourceProvider> providers;

    @Override
    public List<CityDiscoveryCandidate> discoverSupermarkets(String city) {
        Map<String, CityDiscoveryCandidate> deduped = new LinkedHashMap<>();

        for (CityDiscoverySourceProvider provider : providers) {
            for (CityDiscoveryCandidate candidate : provider.discoverSupermarkets(city)) {
                if (candidate == null || candidate.getSupermarketName() == null || candidate.getSupermarketName().isBlank()) {
                    continue;
                }

                String key = normalize(candidate.getSupermarketName());
                CityDiscoveryCandidate existing = deduped.get(key);
                if (existing == null || candidate.getSourceConfidence() > existing.getSourceConfidence()) {
                    deduped.put(key, candidate);
                }
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
