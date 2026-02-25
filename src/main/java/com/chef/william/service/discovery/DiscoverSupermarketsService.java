package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsMeta;
import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import com.chef.william.dto.discovery.SupermarketDiscoveryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscoverSupermarketsService {

    private final CountryDetectionService countryDetectionService;
    private final SupermarketDiscoveryCrawler supermarketDiscoveryCrawler;
    private final SupermarketInspectionService supermarketInspectionService;

    public DiscoverSupermarketsResponse discoverSupermarkets(DiscoverSupermarketsRequest request) {
        CountryResolutionResult countryResolution = countryDetectionService.detectCountryCode(request.city());

        if (!countryResolution.resolved()) {
            return fallback(
                    request.city(),
                    countryResolution,
                    "We couldn’t confidently detect the country for this city. Please try a more specific city name.",
                    "phase0"
            );
        }

        List<DiscoveredSupermarket> discoveredMarkets = supermarketDiscoveryCrawler
                .discover(request.city(), countryResolution.countryCode());

        if (discoveredMarkets.isEmpty()) {
            return fallback(
                    request.city(),
                    countryResolution,
                    "No online supermarkets were discovered for this location yet.",
                    "phase1"
            );
        }

        List<SupermarketInspectionResult> inspectionResults = discoveredMarkets.stream()
                .map(market -> supermarketInspectionService.inspect(market, request.ingredient()))
                .toList();

        boolean hasInspectedMarket = inspectionResults.stream().anyMatch(SupermarketInspectionResult::inspected);
        if (!hasInspectedMarket) {
            return fallback(
                    request.city(),
                    countryResolution,
                    "We couldn’t inspect supermarket results right now. Please try again later.",
                    "phase2"
            );
        }

        List<SupermarketDiscoveryResult> data = inspectionResults.stream()
                .map(result -> new SupermarketDiscoveryResult(
                        result.supermarketName(),
                        result.homepage(),
                        result.ingredientSearchUrl(),
                        result.available(),
                        result.confidence()
                ))
                .toList();

        return new DiscoverSupermarketsResponse(
                "success",
                "Phase 2 completed: supermarket ingredient inspection finished.",
                data,
                new DiscoverSupermarketsMeta(
                        request.city(),
                        countryResolution.countryCode(),
                        countryResolution.confidence(),
                        "phase2"
                )
        );
    }

    private DiscoverSupermarketsResponse fallback(
            String city,
            CountryResolutionResult countryResolution,
            String message,
            String phase
    ) {
        return new DiscoverSupermarketsResponse(
                "fallback",
                message,
                List.of(),
                new DiscoverSupermarketsMeta(city, countryResolution.countryCode(), countryResolution.confidence(), phase)
        );
    }
}
