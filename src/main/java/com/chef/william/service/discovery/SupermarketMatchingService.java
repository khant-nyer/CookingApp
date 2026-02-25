package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.SupermarketDiscoveryResult;

import java.util.List;

public interface SupermarketMatchingService {
    List<SupermarketDiscoveryResult> matchAndRank(String ingredient, List<SupermarketInspectionResult> inspections);
}
