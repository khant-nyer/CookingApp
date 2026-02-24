package com.chef.william.service.discovery;

import com.chef.william.config.CityDiscoveryProperties;
import com.chef.william.config.SupermarketDiscoveryProperties;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.service.discovery.provider.CityDiscoveryCandidate;
import com.chef.william.service.discovery.provider.CityDiscoveryProvider;
import com.chef.william.service.discovery.verification.CatalogVerificationResult;
import com.chef.william.service.discovery.verification.SupermarketCatalogVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupermarketDiscoveryService {

    private final CitySupermarketRepository citySupermarketRepository;
    private final CityDiscoveryProvider cityDiscoveryProvider;
    private final CityDiscoveryProperties cityDiscoveryProperties;
    private final SupermarketDiscoveryProperties supermarketDiscoveryProperties;
    private final SupermarketCatalogVerifier supermarketCatalogVerifier;

    @Transactional
    public List<SupermarketDiscoveryDTO> discover(String city, String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new BusinessException("Ingredient name is required for supermarket discovery");
        }

        String effectiveCity = resolveCity(city);
        List<CitySupermarket> cachedMarkets = citySupermarketRepository.findByCityIgnoreCase(effectiveCity);

        Map<String, CandidateMarket> candidates = new LinkedHashMap<>();
        addCachedCandidates(candidates, cachedMarkets);
        addLiveDiscoveredCandidates(candidates, effectiveCity);
        addFallbackSeedCandidates(candidates, effectiveCity);

        if (candidates.isEmpty()) {
            throw new BusinessException("No verified supermarkets found for city: " + effectiveCity);
        }

        List<SupermarketDiscoveryDTO> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (CandidateMarket candidate : candidates.values()) {
            CitySupermarket market = upsertCacheRow(effectiveCity, candidate, now);
            String searchUrl = buildCatalogUrl(market.getCatalogSearchUrl(), ingredientName);
            String crawlTarget = !searchUrl.isBlank() ? searchUrl : market.getOfficialWebsite();

            CatalogVerificationResult verification = supermarketCatalogVerifier.verifyIngredient(crawlTarget, ingredientName);
            if (!verification.isMatched() && !supportsCatalogSearchTemplate(market)) {
                market.setVerificationStatus("UNMATCHED");
                market.setLastFailureReason("NO_MATCH_ON_CRAWL");
                market.setLastVerifiedAt(now);
                citySupermarketRepository.save(market);
                continue;
            }

            market.setVerificationStatus("MATCHED");
            market.setLastFailureReason("");
            market.setLastVerifiedAt(now);
            market.setVerificationConfidence(candidate.confidence);
            citySupermarketRepository.save(market);

            String matchSource = verification.isMatched() ? verification.getMatchSource() : "CATALOG_SEARCH_URL_TEMPLATE";
            results.add(new SupermarketDiscoveryDTO(
                    effectiveCity,
                    market.getSupermarketName(),
                    market.getOfficialWebsite(),
                    crawlTarget,
                    true,
                    matchSource,
                    "DB_CACHE",
                    now
            ));
        }

        if (results.isEmpty()) {
            throw new BusinessException("No verified supermarket matches found for ingredient '"
                    + ingredientName.trim() + "' in city: " + effectiveCity);
        }

        return results;
    }

    private void addLiveDiscoveredCandidates(Map<String, CandidateMarket> candidates, String city) {
        double minConfidence = Math.max(0.35, cityDiscoveryProperties.getMinConfidenceToPersist() - 0.20);
        for (CityDiscoveryCandidate candidate : cityDiscoveryProvider.discoverSupermarkets(city)) {
            if (candidate == null || candidate.getSupermarketName() == null || candidate.getSupermarketName().isBlank()) {
                continue;
            }
            if (candidate.getSourceConfidence() < minConfidence) {
                continue;
            }

            String website = normalizeUrl(candidate.getWebsite());
            String key = normalize(candidate.getSupermarketName());
            CandidateMarket existing = candidates.get(key);
            if (existing == null || candidate.getSourceConfidence() > existing.confidence) {
                candidates.put(key, new CandidateMarket(candidate.getSupermarketName().trim(), website, website,
                        candidate.getSourceConfidence(), "LIVE_DISCOVERY"));
            }
        }
    }

    private void addCachedCandidates(Map<String, CandidateMarket> candidates, List<CitySupermarket> cached) {
        LocalDateTime now = LocalDateTime.now();
        for (CitySupermarket market : cached) {
            String key = normalize(market.getSupermarketName());
            double confidence = market.getVerificationConfidence() == null ? 0.6 : market.getVerificationConfidence();
            if (market.getTtlExpiresAt() != null && market.getTtlExpiresAt().isBefore(now)) {
                confidence = Math.max(0.4, confidence - 0.2);
            }
            candidates.putIfAbsent(key, new CandidateMarket(
                    market.getSupermarketName(),
                    normalizeUrl(market.getOfficialWebsite()),
                    normalizeUrl(market.getCatalogSearchUrl()),
                    confidence,
                    "CACHE"
            ));
        }
    }

    private void addFallbackSeedCandidates(Map<String, CandidateMarket> candidates, String city) {
        for (SupermarketDiscoveryProperties.FallbackMarket fallback : supermarketDiscoveryProperties.getFallbackMarkets()) {
            if (!cityMatches(city, fallback.getCity())) {
                continue;
            }
            if (fallback.getSupermarketName() == null || fallback.getSupermarketName().isBlank()) {
                continue;
            }

            String key = normalize(fallback.getSupermarketName());
            candidates.putIfAbsent(key, new CandidateMarket(
                    fallback.getSupermarketName().trim(),
                    normalizeUrl(fallback.getOfficialWebsite()),
                    normalizeUrl(fallback.getCatalogSearchUrl()),
                    0.55,
                    "FALLBACK_BOOTSTRAP"
            ));
        }
    }

    private CitySupermarket upsertCacheRow(String city, CandidateMarket candidate, LocalDateTime now) {
        Optional<CitySupermarket> existing = citySupermarketRepository
                .findFirstByCityIgnoreCaseAndSupermarketNameIgnoreCase(city, candidate.name);

        CitySupermarket market = existing.orElseGet(CitySupermarket::new);
        market.setCity(city);
        market.setSupermarketName(candidate.name);
        market.setNormalizedCity(normalize(city));
        market.setNormalizedSupermarketName(normalize(candidate.name));
        market.setOfficialWebsite(candidate.website);
        market.setCatalogSearchUrl(candidate.catalogUrl.isBlank() ? candidate.website : candidate.catalogUrl);
        market.setCanonicalDomain(extractHost(candidate.website));
        market.setSourceProvider(candidate.source);
        market.setLastDiscoveryAt(now);
        market.setVerificationConfidence(candidate.confidence);
        market.setWebsiteResolverConfidence(candidate.website.isBlank() ? 0.0 : 0.8);
        market.setTtlExpiresAt(now.plusSeconds(Math.max(300, cityDiscoveryProperties.getMarketCacheTtlSeconds())));
        market.setNotes("CACHE_ACCELERATION_ONLY");
        return citySupermarketRepository.save(market);
    }

    private String resolveCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new BusinessException("City is required for supermarket discovery");
        }
        return city.trim();
    }

    private boolean supportsCatalogSearchTemplate(CitySupermarket market) {
        if (market == null) {
            return false;
        }
        String catalogSearchUrl = market.getCatalogSearchUrl();
        return catalogSearchUrl != null && catalogSearchUrl.contains("{ingredient}");
    }

    private String buildCatalogUrl(String baseCatalogUrl, String ingredientName) {
        if (baseCatalogUrl == null || baseCatalogUrl.isBlank()) {
            return "";
        }

        String encodedIngredient = URLEncoder.encode(ingredientName.trim(), StandardCharsets.UTF_8);
        if (baseCatalogUrl.contains("{ingredient}")) {
            return baseCatalogUrl.replace("{ingredient}", encodedIngredient);
        }

        if (baseCatalogUrl.contains("?")) {
            return baseCatalogUrl + "&q=" + encodedIngredient;
        }

        return baseCatalogUrl + "?q=" + encodedIngredient;
    }

    private boolean cityMatches(String city, String configuredCity) {
        return configuredCity != null && configuredCity.trim().equalsIgnoreCase(city.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private String extractHost(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(value).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return "";
        }
    }

    private record CandidateMarket(String name, String website, String catalogUrl, double confidence, String source) {
    }
}
