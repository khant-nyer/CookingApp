package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SearchEngineSupermarketDiscoveryClient {

    @Value("${app.discovery.max-supermarkets:10}")
    private int maxSupermarkets;

    @Value("${app.discovery.user-agent:CookingApp/1.0 (supermarket-discovery-service)}")
    private String userAgent;

    public List<SupermarketDTO> discoverBySearch(String city, String country) {
        String query = "popular supermarkets in " + city;
        String url = "https://duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        try {
            Document document = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(15000)
                    .get();

            Map<String, SupermarketDTO> unique = new LinkedHashMap<>();
            for (Element titleElement : document.select("a.result__a")) {
                String normalized = titleElement.text().replaceAll("\\s+-\\s+.*$", "").trim();
                if (normalized.isBlank()) {
                    continue;
                }

                String key = normalized.toLowerCase();
                if (unique.containsKey(key)) {
                    continue;
                }

                unique.put(key, SupermarketDTO.builder()
                        .name(normalized)
                        .city(city)
                        .country(country)
                        .source("SEARCH_ENGINE_FALLBACK")
                        .build());

                if (unique.size() >= maxSupermarkets) {
                    break;
                }
            }

            return new ArrayList<>(unique.values());
        } catch (Exception e) {
            log.warn("Search fallback failed for city {}: {}", city, e.getMessage());
            return List.of();
        }
    }
}
