package com.chef.william.service.discovery;

public record DiscoveredSupermarket(
        String name,
        String homepage,
        String source,
        String confidence
) {
}
