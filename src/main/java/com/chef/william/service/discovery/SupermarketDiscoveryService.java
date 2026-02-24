package com.chef.william.service.discovery;

import com.chef.william.config.SupermarketDiscoveryProperties;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.exception.ResourceNotFoundException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.model.User;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.repository.UserRepository;
import com.chef.william.service.crawler.SupermarketCrawlerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SupermarketDiscoveryService {

    private final CitySupermarketRepository citySupermarketRepository;
    private final UserRepository userRepository;
    private final SupermarketCrawlerClient supermarketCrawlerClient;
    private final SupermarketDiscoveryProperties discoveryProperties;

    @Transactional
    public List<SupermarketDiscoveryDTO> discover(Long userId, String city, String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new BusinessException("Ingredient name is required for supermarket discovery");
        }

        String effectiveCity = resolveCity(userId, city);
        List<CitySupermarket> persistedMarkets = citySupermarketRepository.findByCityIgnoreCase(effectiveCity.trim());

        boolean usingFallback = persistedMarkets.isEmpty();
        List<CitySupermarket> discoveryMarkets = usingFallback
                ? getFallbackCitySupermarkets(effectiveCity)
                : persistedMarkets;

        List<SupermarketDiscoveryDTO> results = new ArrayList<>();
        List<CitySupermarket> matchedFromFallback = new ArrayList<>();

        for (CitySupermarket market : discoveryMarkets) {
            String searchUrl = buildCatalogUrl(market.getCatalogSearchUrl(), ingredientName);
            String crawlTarget = !searchUrl.isBlank() ? searchUrl : market.getOfficialWebsite();
            boolean crawlMatched = supermarketCrawlerClient.webpageContainsIngredient(crawlTarget, ingredientName);
            boolean urlMatched = urlContainsIngredient(crawlTarget, ingredientName);
            boolean matched = crawlMatched || urlMatched;
            String matchSource = crawlMatched ? "OFFICIAL_WEB_CRAWL"
                    : (urlMatched ? "CATALOG_URL_QUERY_MATCH" : "NO_MATCH_ON_CRAWL");

            results.add(new SupermarketDiscoveryDTO(
                    effectiveCity,
                    market.getSupermarketName(),
                    market.getOfficialWebsite(),
                    crawlTarget,
                    matched,
                    matchSource,
                    usingFallback ? "FALLBACK" : "DB",
                    LocalDateTime.now()
            ));

            if (usingFallback && matched) {
                matchedFromFallback.add(market);
            }
        }

        if (!matchedFromFallback.isEmpty()) {
            saveDiscoveredSupermarkets(effectiveCity, matchedFromFallback);
        }

        return results;
    }

    private String resolveCity(Long userId, String city) {
        if (city != null && !city.trim().isEmpty()) {
            return city.trim();
        }

        if (userId == null) {
            throw new BusinessException("City is required when userId is not provided");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getCity() == null || user.getCity().isBlank()) {
            throw new BusinessException("User city is not set for user id: " + userId);
        }

        return user.getCity().trim();
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

    private List<CitySupermarket> getFallbackCitySupermarkets(String city) {
        String normalizedCity = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);

        List<SupermarketDiscoveryProperties.FallbackMarket> citySpecific = discoveryProperties.getFallbackMarkets().stream()
                .filter(entry -> entry.getCity() != null && entry.getCity().trim().toLowerCase(Locale.ROOT).equals(normalizedCity))
                .toList();

        List<SupermarketDiscoveryProperties.FallbackMarket> seeds = citySpecific.isEmpty()
                ? deduplicateFallbackMarkets(discoveryProperties.getFallbackMarkets())
                : citySpecific;

        return seeds.stream()
                .map(entry -> {
                    CitySupermarket market = new CitySupermarket();
                    market.setCity(city.trim());
                    market.setSupermarketName(entry.getSupermarketName());
                    market.setOfficialWebsite(entry.getOfficialWebsite());
                    market.setCatalogSearchUrl(entry.getCatalogSearchUrl());
                    market.setNotes(entry.getNotes());
                    return market;
                })
                .toList();
    }

    private List<SupermarketDiscoveryProperties.FallbackMarket> deduplicateFallbackMarkets(
            List<SupermarketDiscoveryProperties.FallbackMarket> fallbackMarkets) {
        Map<String, SupermarketDiscoveryProperties.FallbackMarket> deduplicated = new LinkedHashMap<>();
        for (SupermarketDiscoveryProperties.FallbackMarket market : fallbackMarkets) {
            String key = (market.getSupermarketName() == null ? "" : market.getSupermarketName().trim().toLowerCase(Locale.ROOT))
                    + "|"
                    + (market.getOfficialWebsite() == null ? "" : market.getOfficialWebsite().trim().toLowerCase(Locale.ROOT));
            deduplicated.putIfAbsent(key, market);
        }
        return deduplicated.values().stream().toList();
    }

    private void saveDiscoveredSupermarkets(String city, List<CitySupermarket> markets) {
        List<CitySupermarket> toSave = markets.stream()
                .filter(market -> !citySupermarketRepository
                        .existsByCityIgnoreCaseAndSupermarketNameIgnoreCase(city, market.getSupermarketName()))
                .peek(market -> market.setId(null))
                .peek(market -> market.setCity(city))
                .toList();

        if (!toSave.isEmpty()) {
            citySupermarketRepository.saveAll(toSave);
        }
    }

    private boolean urlContainsIngredient(String url, String ingredientName) {
        if (url == null || url.isBlank() || ingredientName == null || ingredientName.isBlank()) {
            return false;
        }

        String normalizedUrl = normalizeForMatch(url);
        return Stream.of(ingredientName.trim().split("\\s+"))
                .map(this::normalizeForMatch)
                .filter(token -> !token.isBlank())
                .allMatch(normalizedUrl::contains);
    }

    private String normalizeForMatch(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("+", " ")
                .replace("%20", " ")
                .replaceAll("[^a-z0-9]+", " ");
    }
}
