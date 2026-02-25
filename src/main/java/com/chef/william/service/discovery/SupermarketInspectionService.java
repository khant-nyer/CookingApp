package com.chef.william.service.discovery;

public interface SupermarketInspectionService {
    SupermarketInspectionResult inspect(DiscoveredSupermarket supermarket, String ingredient);
}
