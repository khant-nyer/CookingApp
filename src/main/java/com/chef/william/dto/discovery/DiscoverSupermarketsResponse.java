package com.chef.william.dto.discovery;

import java.util.List;

public record DiscoverSupermarketsResponse(
        String status,
        String message,
        List<SupermarketDiscoveryResult> data,
        DiscoverSupermarketsMeta meta
) {
}
