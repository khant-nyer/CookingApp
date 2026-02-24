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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

        DiscoveryStats stats = new DiscoveryStats(effectiveCity, ingredientName.trim());
        Map<String, CandidateMarket> candidates = new LinkedHashMap<>();
        addCachedCandidates(candidates, cachedMarkets, stats);
        addLiveDiscoveredCandidates(candidates, effectiveCity, stats);
        addFallbackSeedCandidates(candidates, effectiveCity, stats);

        if (candidates.isEmpty()) {
            log.warn("DISCOVERY_FAIL city={} ingredient={} reason=NO_CANDIDATES", effectiveCity, ingredientName.trim());
            throw new BusinessException("No verified supermarkets found for city: " + effectiveCity);
        }

        List<RankedResult> rankedResults = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (CandidateMarket candidate : candidates.values()) {
            CitySupermarket market = upsertCacheRow(effectiveCity, candidate, now);
            String searchUrl = buildCatalogUrl(market.getCatalogSearchUrl(), ingredientName);
            String crawlTarget = !searchUrl.isBlank() ? searchUrl : market.getOfficialWebsite();

            CatalogVerificationResult verification = supermarketCatalogVerifier.verifyIngredient(crawlTarget, ingredientName);
            if (!verification.isMatched() && !supportsCatalogSearchTemplate(market)) {
                market.setVerificationStatus("UNMATCHED");
                market.setLastFailureReason(verification.getFailureReason());
                market.setLastVerifiedAt(now);
                citySupermarketRepository.save(market);
                stats.unmatched++;
                log.info("DISCOVERY_REJECT city={} supermarket={} reason={} source={}",
                        effectiveCity,
                        market.getSupermarketName(),
                        verification.getFailureReason(),
                        market.getSourceProvider());
                continue;
            }

            market.setVerificationStatus("MATCHED");
            market.setLastFailureReason("");
            market.setLastVerifiedAt(now);
            double finalConfidence = Math.max(candidate.confidence, verification.getConfidence());
            market.setVerificationConfidence(finalConfidence);
            citySupermarketRepository.save(market);

            String matchSource = verification.isMatched() ? verification.getMatchSource() : "CATALOG_SEARCH_URL_TEMPLATE";
            SupermarketDiscoveryDTO dto = new SupermarketDiscoveryDTO(
                    effectiveCity,
                    market.getSupermarketName(),
                    market.getOfficialWebsite(),
                    crawlTarget,
                    true,
                    matchSource,
                    "DB_CACHE",
                    now
            );
            rankedResults.add(new RankedResult(dto, rankScore(candidate, market, verification, now)));
            stats.matched++;
        }

        if (rankedResults.isEmpty()) {
            log.warn("DISCOVERY_FAIL city={} ingredient={} reason=NO_VERIFIED_MATCHES", effectiveCity, ingredientName.trim());
            throw new BusinessException("No verified supermarket matches found for ingredient '"
                    + ingredientName.trim() + "' in city: " + effectiveCity);
        }

        log.info("DISCOVERY_SUMMARY city={} ingredient={} candidates={} cacheCandidates={} liveCandidates={} " +
                        "bootstrapSeeds={} matched={} unmatched={}",
                stats.city, stats.ingredient, candidates.size(), stats.cacheCandidates,
                stats.liveCandidates, stats.bootstrapSeeds, stats.matched, stats.unmatched);

        return rankedResults.stream()
                .sorted(Comparator.comparingDouble(RankedResult::score).reversed())
                .map(RankedResult::dto)
                .toList();
    }

    private void addLiveDiscoveredCandidates(Map<String, CandidateMarket> candidates, String city, DiscoveryStats stats) {
        double minConfidence = Math.max(0.35, cityDiscoveryProperties.getMinConfidenceToPersist() - 0.20);
        for (CityDiscoveryCandidate candidate : cityDiscoveryProvider.discoverSupermarkets(city)) {
            if (candidate == null || candidate.getSupermarketName() == null || candidate.getSupermarketName().isBlank()) {
                stats.rejectedCandidates++;
                continue;
            }
            if (candidate.getSourceConfidence() < minConfidence) {
                stats.rejectedCandidates++;
                log.debug("DISCOVERY_CANDIDATE_REJECT city={} supermarket={} reason=LOW_CONFIDENCE confidence={}",
                        city, candidate.getSupermarketName(), candidate.getSourceConfidence());
                continue;
            }

            String website = normalizeUrl(candidate.getWebsite());
            if (isBlockedCandidate(candidate.getSupermarketName(), website)) {
                stats.rejectedCandidates++;
                log.debug("DISCOVERY_CANDIDATE_REJECT city={} supermarket={} reason=BLOCKED_DOMAIN",
                        city, candidate.getSupermarketName());
                continue;
            }
            String key = normalize(candidate.getSupermarketName());
            CandidateMarket existing = candidates.get(key);
            CandidateMarket incoming = new CandidateMarket(candidate.getSupermarketName().trim(), website, website,
                    candidate.getSourceConfidence(), "LIVE_DISCOVERY");
            if (shouldReplaceLiveCandidate(existing, incoming)) {
                candidates.put(key, incoming);
                stats.liveCandidates++;
            }
        }
    }

    private void addCachedCandidates(Map<String, CandidateMarket> candidates,
                                     List<CitySupermarket> cached,
                                     DiscoveryStats stats) {
        LocalDateTime now = LocalDateTime.now();
        for (CitySupermarket market : cached) {
            if (isBlockedCandidate(market.getSupermarketName(), market.getOfficialWebsite())) {
                stats.rejectedCandidates++;
                continue;
            }
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
            stats.cacheCandidates++;
        }
    }

    private void addFallbackSeedCandidates(Map<String, CandidateMarket> candidates,
                                           String city,
                                           DiscoveryStats stats) {
        for (SupermarketDiscoveryProperties.FallbackMarket fallback : supermarketDiscoveryProperties.getFallbackMarkets()) {
            if (!cityMatches(city, fallback.getCity())) {
                continue;
            }
            if (fallback.getSupermarketName() == null || fallback.getSupermarketName().isBlank()) {
                continue;
            }

            if (isBlockedCandidate(fallback.getSupermarketName(), fallback.getOfficialWebsite())) {
                stats.rejectedCandidates++;
                continue;
            }
            String key = normalize(fallback.getSupermarketName());
            boolean isNew = !candidates.containsKey(key);
            candidates.putIfAbsent(key, new CandidateMarket(
                    fallback.getSupermarketName().trim(),
                    normalizeUrl(fallback.getOfficialWebsite()),
                    normalizeUrl(fallback.getCatalogSearchUrl()),
                    0.45,
                    "BOOTSTRAP_SEED"
            ));
            if (isNew) {
                stats.bootstrapSeeds++;
            }
        }
    }

    private double rankScore(CandidateMarket candidate,
                             CitySupermarket market,
                             CatalogVerificationResult verification,
                             LocalDateTime now) {
        double score = Math.max(candidate.confidence, verification.getConfidence());

        if (market.getLastVerifiedAt() != null) {
            long ageHours = java.time.Duration.between(market.getLastVerifiedAt(), now).toHours();
            if (ageHours <= 24) {
                score += 0.08;
            } else if (ageHours > 72) {
                score -= 0.08;
            }
        }

        if ("LIVE_DISCOVERY".equalsIgnoreCase(candidate.source)) {
            score += 0.04;
        }
        if ("BOOTSTRAP_SEED".equalsIgnoreCase(candidate.source)) {
            score -= 0.06;
        }
        if (verification.isMatched() && "STRUCTURED_PRODUCT_SCRAPE".equalsIgnoreCase(verification.getMatchSource())) {
            score += 0.06;
        }

        return Math.min(0.99, Math.max(0.0, score));
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

    private boolean isBlockedCandidate(String supermarketName, String website) {
        String normalizedName = normalize(supermarketName);
        String host = extractHost(normalizeUrl(website));

        if (normalizedName.contains("duckduckgo") || normalizedName.contains("google") || normalizedName.contains("bing")) {
            return true;
        }

        return host.contains("duckduckgo.com")
                || host.contains("google.")
                || host.contains("bing.com")
                || host.contains("search.yahoo.com")
                || host.contains("yahoo.com");
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

    private record RankedResult(SupermarketDiscoveryDTO dto, double score) {
    }

    private record CandidateMarket(String name, String website, String catalogUrl, double confidence, String source) {
    }

    private static final class DiscoveryStats {
        private final String city;
        private final String ingredient;
        private int cacheCandidates;
        private int liveCandidates;
        private int bootstrapSeeds;
        private int rejectedCandidates;
        private int matched;
        private int unmatched;

        private DiscoveryStats(String city, String ingredient) {
            this.city = city;
            this.ingredient = ingredient;
        }
    }
}
