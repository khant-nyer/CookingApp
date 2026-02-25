package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenStreetMapSupermarketDiscoveryClient {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.discovery.max-supermarkets:10}")
    private int maxSupermarkets;

    @Value("${app.discovery.user-agent:CookingApp/1.0 (supermarket-discovery-service)}")
    private String userAgent;

    public Optional<CityContext> resolveCity(String city) {
        String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                .queryParam("city", city)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 1)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        JsonNode root = readJsonArray(url);
        if (root == null || root.isEmpty()) {
            String fallbackUrl = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                    .queryParam("q", city)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", 1)
                    .queryParam("limit", 1)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            root = readJsonArray(fallbackUrl);
        }

        if (root == null || root.isEmpty()) {
            return Optional.empty();
        }

        JsonNode first = root.get(0);
        JsonNode address = first.path("address");

        return Optional.of(new CityContext(
                city,
                address.path("country").asText(null),
                address.path("country_code").asText(null)
        ));
    }

    public List<SupermarketDTO> discoverSupermarkets(String city, String countryName) {
        String query = """
                [out:json][timeout:25];
                area[\"name\"=\"%s\"][\"boundary\"=\"administrative\"]->.searchArea;
                (
                  node[\"shop\"=\"supermarket\"](area.searchArea);
                  way[\"shop\"=\"supermarket\"](area.searchArea);
                  relation[\"shop\"=\"supermarket\"](area.searchArea);
                );
                out tags center %d;
                """.formatted(escapeOverpassValue(city), maxSupermarkets * 3);

        String url = UriComponentsBuilder.fromHttpUrl(OVERPASS_URL)
                .queryParam("data", query)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        JsonNode root = readJsonObject(url);
        if (root == null || !root.has("elements")) {
            return List.of();
        }

        Map<String, SupermarketDTO> unique = new LinkedHashMap<>();
        for (JsonNode element : root.path("elements")) {
            JsonNode tags = element.path("tags");
            String name = textOrNull(tags, "name");
            if (name == null || name.isBlank()) {
                name = textOrNull(tags, "brand");
            }
            if (name == null || name.isBlank()) {
                continue;
            }

            String key = name.trim().toLowerCase();
            if (unique.containsKey(key)) {
                continue;
            }

            String address = compactAddress(tags);
            Double lat = element.path("lat").isNumber() ? element.path("lat").asDouble() :
                    (element.path("center").path("lat").isNumber() ? element.path("center").path("lat").asDouble() : null);
            Double lon = element.path("lon").isNumber() ? element.path("lon").asDouble() :
                    (element.path("center").path("lon").isNumber() ? element.path("center").path("lon").asDouble() : null);

            unique.put(key, SupermarketDTO.builder()
                    .name(name.trim())
                    .city(city)
                    .country(countryName)
                    .address(address)
                    .latitude(lat)
                    .longitude(lon)
                    .source("OPENSTREETMAP")
                    .build());

            if (unique.size() >= maxSupermarkets) {
                break;
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String compactAddress(JsonNode tags) {
        Map<String, String> parts = new HashMap<>();
        parts.put("street", textOrNull(tags, "addr:street"));
        parts.put("housenumber", textOrNull(tags, "addr:housenumber"));
        parts.put("city", textOrNull(tags, "addr:city"));
        parts.put("postcode", textOrNull(tags, "addr:postcode"));

        return parts.values().stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private JsonNode readJsonArray(String url) {
        return execute(url);
    }

    private JsonNode readJsonObject(String url) {
        return execute(url);
    }

    private JsonNode execute(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", userAgent);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("Discovery API call failed for URL {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String textOrNull(JsonNode node, String key) {
        String value = node.path(key).asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private String escapeOverpassValue(String value) {
        return value.replace("\"", "\\\"");
    }

    public record CityContext(String city, String countryName, String countryCode) {
    }
}
