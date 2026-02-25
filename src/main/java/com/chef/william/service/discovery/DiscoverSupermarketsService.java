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

    public DiscoverSupermarketsResponse discoverSupermarkets(DiscoverSupermarketsRequest request) {
        CountryResolutionResult countryResolution = countryDetectionService.detectCountryCode(request.city());

        if (!countryResolution.resolved()) {
            return fallback(
                    request.city(),
                    countryResolution,
                    "We couldn’t confidently detect the country for this city. Please try a more specific city name."
            );
        }

        return new DiscoverSupermarketsResponse(
                "success",
                "Phase 0 completed: country detected. Discovery crawl starts in Phase 1.",
                List.<SupermarketDiscoveryResult>of(),
                new DiscoverSupermarketsMeta(
                        request.city(),
                        countryResolution.countryCode(),
                        countryResolution.confidence(),
                        "phase0"
                )
        );
    }

    private DiscoverSupermarketsResponse fallback(String city, CountryResolutionResult countryResolution, String message) {
        return new DiscoverSupermarketsResponse(
                "fallback",
                message,
                List.of(),
                new DiscoverSupermarketsMeta(city, null, countryResolution.confidence(), "phase0")
        );
    }
}
