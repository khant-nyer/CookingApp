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

        List<SupermarketDiscoveryResult> data = discoveredMarkets.stream()
                .map(market -> new SupermarketDiscoveryResult(
                        market.name(),
                        market.homepage(),
                        null,
                        false,
                        market.confidence()
                ))
                .toList();

        return new DiscoverSupermarketsResponse(
                "success",
                "Phase 1 completed: supermarket candidates discovered.",
                data,
                new DiscoverSupermarketsMeta(
                        request.city(),
                        countryResolution.countryCode(),
                        countryResolution.confidence(),
                        "phase1"
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
