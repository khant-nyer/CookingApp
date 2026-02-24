package com.chef.william.service.discovery;

import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.repository.CitySupermarketRepository;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SupermarketDiscoveryService {

    private final CitySupermarketRepository citySupermarketRepository;
    private final SupermarketCrawlerClient supermarketCrawlerClient;

    @Transactional
    public List<SupermarketDiscoveryDTO> discover(String city, String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            throw new BusinessException("Ingredient name is required for supermarket discovery");
        }

        String effectiveCity = resolveCity(city);
        List<CitySupermarket> persistedMarkets = citySupermarketRepository.findByCityIgnoreCase(effectiveCity.trim());

        if (persistedMarkets.isEmpty()) {
            throw new BusinessException("No verified supermarkets found for city: " + effectiveCity);
        }

        List<SupermarketDiscoveryDTO> results = new ArrayList<>();

        for (CitySupermarket market : persistedMarkets) {
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
                    "DB",
                    LocalDateTime.now()
            ));

        }

        return results;
    }

    private String resolveCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new BusinessException("City is required for supermarket discovery");
        }

        return city.trim();
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
