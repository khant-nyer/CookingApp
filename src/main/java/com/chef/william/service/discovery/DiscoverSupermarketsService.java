package com.chef.william.service.discovery;

import com.chef.william.dto.discovery.DiscoverSupermarketsMeta;
import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import com.chef.william.dto.discovery.SupermarketDiscoveryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoverSupermarketsService {

    private final CountryDetectionService countryDetectionService;
    private final SupermarketDiscoveryCrawler supermarketDiscoveryCrawler;
    private final SupermarketInspectionService supermarketInspectionService;
    private final SupermarketMatchingService supermarketMatchingService;

    public DiscoverSupermarketsResponse discoverSupermarkets(DiscoverSupermarketsRequest request) {
        CountryResolutionResult countryResolution = countryDetectionService.detectCountryCode(request.city());

        if (!countryResolution.resolved()) {
            log.info("Phase0 fallback city='{}' reason='{}'", request.city(), countryResolution.reason());
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
            log.info("Phase1 fallback city='{}' country='{}'", request.city(), countryResolution.countryCode());
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
            log.info("Phase2 fallback city='{}' candidates={}", request.city(), discoveredMarkets.size());
            return fallback(
                    request.city(),
                    countryResolution,
                    "We couldn’t inspect supermarket results right now. Please try again later.",
                    "phase2"
            );
        }

        List<SupermarketDiscoveryResult> matchedMarkets = supermarketMatchingService
                .matchAndRank(request.ingredient(), inspectionResults);

        if (matchedMarkets.isEmpty()) {
            log.info("Phase3 fallback city='{}' ingredient='{}' inspectedCount={}",
                    request.city(), request.ingredient(), inspectionResults.size());
            return fallback(
                    request.city(),
                    countryResolution,
                    "No supermarkets currently show strong matches for this ingredient in your area.",
                    "phase3"
            );
        }

        log.info("Discovery success city='{}' country='{}' matched={}",
                request.city(), countryResolution.countryCode(), matchedMarkets.size());
        return new DiscoverSupermarketsResponse(
                "success",
                "Phase 3 completed: supermarkets matched and ranked.",
                matchedMarkets,
                new DiscoverSupermarketsMeta(
                        request.city(),
                        countryResolution.countryCode(),
                        countryResolution.confidence(),
                        "phase3"
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
