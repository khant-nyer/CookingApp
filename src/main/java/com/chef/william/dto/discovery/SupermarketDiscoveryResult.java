package com.chef.william.dto.discovery;

public record SupermarketDiscoveryResult(
        String name,
        String homepage,
        String ingredientSearchUrl,
        boolean available,
        String confidence
) {
}
