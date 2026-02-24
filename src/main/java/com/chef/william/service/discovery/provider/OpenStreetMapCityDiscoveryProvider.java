package com.chef.william.service.discovery.provider;

import com.chef.william.config.CityDiscoveryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenStreetMapCityDiscoveryProvider implements CityDiscoverySourceProvider {

    private final CityDiscoveryProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, CacheEntry> cache = new HashMap<>();
    private Instant lastRequestAt = Instant.EPOCH;


    @Override
    public String sourceName() {
        return "osm";
    }
    @Override
    public synchronized List<CityDiscoveryCandidate> discoverSupermarkets(String city) {
        String normalizedCity = normalizeCity(city);
        if (normalizedCity.isBlank()) {
            return List.of();
        }

        CacheEntry cached = cache.get(normalizedCity);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.candidates();
        }

        List<CityDiscoveryCandidate> discovered = requestWithRetry(normalizedCity);
        cache.put(normalizedCity, new CacheEntry(
                discovered,
                Instant.now().plusSeconds(Math.max(60, properties.getCacheTtlSeconds()))
        ));
        return discovered;
    }

    private List<CityDiscoveryCandidate> requestWithRetry(String city) {
        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(6))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                enforceRateLimit();
                return queryOpenStreetMap(restTemplate, city);
            } catch (Exception ex) {
                if (attempt == maxAttempts) {
                    log.warn("City discovery provider failed for city={} after {} attempts", city, maxAttempts, ex);
                    return List.of();
                }
                sleep(properties.getRetryBackoffMs() * attempt);
            }
        }

        return List.of();
    }

    private List<CityDiscoveryCandidate> queryOpenStreetMap(RestTemplate restTemplate, String city) throws Exception {
        String encodedQuery = URLEncoder.encode("supermarket in " + city, StandardCharsets.UTF_8);
        String url = properties.getNominatimBaseUrl()
                + "?format=jsonv2&addressdetails=1&extratags=1&namedetails=1&limit="
                + Math.max(3, properties.getMaxCandidates() * 2)
                + "&q=" + encodedQuery;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add(HttpHeaders.USER_AGENT, "CookingApp/1.0 (city-discovery)");

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return List.of();
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        if (!root.isArray()) {
            return List.of();
        }

        Map<String, CityDiscoveryCandidate> deduped = new LinkedHashMap<>();

        for (JsonNode node : root) {
            String type = node.path("type").asText("").toLowerCase(Locale.ROOT);
            String category = node.path("category").asText("").toLowerCase(Locale.ROOT);
            if (!"supermarket".equals(type) && !("shop".equals(category) && type.contains("market"))) {
                continue;
            }

            String name = pickName(node);
            if (name.isBlank()) {
                continue;
            }

            String website = pickWebsite(node);
            String key = normalize(name);
            if (deduped.containsKey(key)) {
                continue;
            }

            double confidence = computeConfidence(node, city, website);
            deduped.put(key, new CityDiscoveryCandidate(name, website, confidence));
            if (deduped.size() >= Math.max(1, properties.getMaxCandidates())) {
                break;
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private void enforceRateLimit() {
        long minInterval = Math.max(0, properties.getMinRequestIntervalMs());
        if (minInterval == 0) {
            return;
        }

        Instant now = Instant.now();
        long elapsed = Duration.between(lastRequestAt, now).toMillis();
        if (elapsed < minInterval) {
            sleep(minInterval - elapsed);
        }
        lastRequestAt = Instant.now();
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }


    private String pickWebsite(JsonNode node) {
        JsonNode tags = node.path("extratags");
        String website = firstNonBlank(
                tags.path("website").asText(""),
                tags.path("contact:website").asText(""),
                tags.path("url").asText(""),
                tags.path("contact:url").asText("")
        );

        if (website.isBlank()) {
            return "";
        }

        String trimmed = website.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String pickName(JsonNode node) {
        String named = node.path("namedetails").path("name").asText("");
        if (!named.isBlank()) {
            return named.trim();
        }
        String display = node.path("display_name").asText("");
        if (display.contains(",")) {
            return display.substring(0, display.indexOf(',')).trim();
        }
        return display.trim();
    }

    private double computeConfidence(JsonNode node, String city, String website) {
        double confidence = 0.55;

        String display = node.path("display_name").asText("").toLowerCase(Locale.ROOT);
        if (display.contains(city.toLowerCase(Locale.ROOT))) {
            confidence += 0.25;
        }

        if (!website.isBlank() && website.startsWith("http")) {
            confidence += 0.10;
        }

        String importance = node.path("importance").asText("0");
        try {
            double val = Double.parseDouble(importance);
            confidence += Math.min(0.10, Math.max(0, val * 0.1));
        } catch (NumberFormatException ignored) {
            // no-op
        }

        return Math.min(0.99, confidence);
    }

    private String normalizeCity(String city) {
        return city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CacheEntry(List<CityDiscoveryCandidate> candidates, Instant expiresAt) {
    }
}
