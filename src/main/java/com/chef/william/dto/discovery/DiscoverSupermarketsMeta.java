package com.chef.william.dto.discovery;

public record DiscoverSupermarketsMeta(
        String city,
        String resolvedCountryCode,
        String resolutionConfidence,
        String phase
) {
}
