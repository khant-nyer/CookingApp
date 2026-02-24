package com.chef.william.service.discovery;

import com.chef.william.config.CityDiscoveryProperties;
import com.chef.william.config.SupermarketDiscoveryProperties;
import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.service.crawler.SupermarketCrawlerClient;
import com.chef.william.service.discovery.provider.CityDiscoveryCandidate;
import com.chef.william.service.discovery.provider.CityDiscoveryProvider;
import com.chef.william.service.discovery.verification.CatalogVerificationResult;
import com.chef.william.service.discovery.verification.SupermarketCatalogVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupermarketDiscoveryService {

    private final CitySupermarketRepository citySupermarketRepository;
    private final SupermarketCrawlerClient supermarketCrawlerClient;
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
        List<CitySupermarket> persistedMarkets = citySupermarketRepository.findByCityIgnoreCase(effectiveCity.trim());

        if (persistedMarkets.isEmpty()) {
            persistedMarkets = discoverAndPersistCityMarkets(effectiveCity);
        }

        if (persistedMarkets.isEmpty()) {
            throw new BusinessException("No verified supermarkets found for city: " + effectiveCity);
        }

        List<SupermarketDiscoveryDTO> results = new ArrayList<>();

        for (CitySupermarket market : persistedMarkets) {
            String searchUrl = buildCatalogUrl(market.getCatalogSearchUrl(), ingredientName);
            String crawlTarget = !searchUrl.isBlank() ? searchUrl : market.getOfficialWebsite();
            CatalogVerificationResult verification = supermarketCatalogVerifier
                    .verifyIngredient(crawlTarget, ingredientName);

            if (!verification.isMatched() && !supportsCatalogSearchTemplate(market)) {
                continue;
            }

            String matchSource = verification.isMatched()
                    ? verification.getMatchSource()
                    : "CATALOG_SEARCH_URL_TEMPLATE";

            results.add(new SupermarketDiscoveryDTO(
                    effectiveCity,
                    market.getSupermarketName(),
                    market.getOfficialWebsite(),
                    crawlTarget,
                    true,
                    matchSource,
                    "DB",
                    LocalDateTime.now()
            ));
        }

        if (results.isEmpty()) {
            throw new BusinessException("No verified supermarket matches found for ingredient '"
                    + ingredientName.trim() + "' in city: " + effectiveCity);
        }

        return results;
    }

    private List<CitySupermarket> discoverAndPersistCityMarkets(String city) {
        List<CityDiscoveryCandidate> candidates = cityDiscoveryProvider.discoverSupermarkets(city);
        double minConfidence = cityDiscoveryProperties.getMinConfidenceToPersist();

        for (CityDiscoveryCandidate candidate : candidates) {
            if (!isPersistableCandidate(city, candidate, minConfidence)) {
                continue;
            }

            String website = candidate.getWebsite().trim();
            String supermarketName = candidate.getSupermarketName().trim();
            boolean reachable = supermarketCrawlerClient.webpageReachable(website);
            if (!reachable) {
                continue;
            }

            CitySupermarket market = new CitySupermarket();
            market.setCity(city);
            market.setSupermarketName(supermarketName);
            market.setOfficialWebsite(website);
            market.setCatalogSearchUrl(website);
            market.setNotes("CITY_PROVIDER_DISCOVERY verifiedBy=OFFICIAL_WEB_CRAWL confidence="
                    + candidate.getSourceConfidence());
            citySupermarketRepository.save(market);
        }

        seedConfiguredFallbackMarkets(city);

        return citySupermarketRepository.findByCityIgnoreCase(city);
    }

    private void seedConfiguredFallbackMarkets(String city) {
        for (SupermarketDiscoveryProperties.FallbackMarket fallback : supermarketDiscoveryProperties.getFallbackMarkets()) {
            if (!cityMatches(city, fallback.getCity())) {
                continue;
            }

            if (fallback.getSupermarketName() == null || fallback.getSupermarketName().isBlank()) {
                continue;
            }

            if (citySupermarketRepository.existsByCityIgnoreCaseAndSupermarketNameIgnoreCase(
                    city,
                    fallback.getSupermarketName().trim()
            )) {
                continue;
            }

            CitySupermarket market = new CitySupermarket();
            market.setCity(city);
            market.setSupermarketName(fallback.getSupermarketName().trim());
            market.setOfficialWebsite(trimOrEmpty(fallback.getOfficialWebsite()));
            market.setCatalogSearchUrl(trimOrEmpty(fallback.getCatalogSearchUrl()));
            market.setNotes(trimOrEmpty(fallback.getNotes()));
            citySupermarketRepository.save(market);
        }
    }

    private boolean isPersistableCandidate(String city, CityDiscoveryCandidate candidate, double minConfidence) {
        if (candidate == null || candidate.getSupermarketName() == null || candidate.getSupermarketName().isBlank()) {
            return false;
        }
        if (candidate.getWebsite() == null || candidate.getWebsite().isBlank()) {
            return false;
        }
        String website = candidate.getWebsite().trim().toLowerCase();
        if (!website.startsWith("http://") && !website.startsWith("https://")) {
            return false;
        }
        if (candidate.getSourceConfidence() < minConfidence) {
            return false;
        }
        return !citySupermarketRepository.existsByCityIgnoreCaseAndSupermarketNameIgnoreCase(city, candidate.getSupermarketName());
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
        if (configuredCity == null) {
            return false;
        }
        return configuredCity.trim().equalsIgnoreCase(city.trim());
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
