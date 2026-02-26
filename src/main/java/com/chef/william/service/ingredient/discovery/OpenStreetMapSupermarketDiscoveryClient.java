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
import java.util.Comparator;
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

    @Value("${app.discovery.search-radius-meters:12000}")
    private int searchRadiusMeters;

    @Value("${app.discovery.fetch-multiplier:20}")
    private int fetchMultiplier;

    public Optional<CityContext> resolveCity(String city) {
        String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                .queryParam("city", city)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("accept-language", "en")
                .queryParam("limit", 1)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        JsonNode root = readJsonArray(url);
        if (root == null || root.isEmpty()) {
            String fallbackUrl = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                    .queryParam("q", city)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", 1)
                    .queryParam("accept-language", "en")
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
                address.path("country_code").asText(null),
                first.path("lat").asText(null),
                first.path("lon").asText(null)
        ));
    }

    public List<SupermarketDTO> discoverSupermarkets(String city, String countryName, CityContext cityContext) {
        String query = buildOverpassQuery(city, cityContext);

        JsonNode root = executeOverpassQuery(query);
        if (root == null || !root.has("elements")) {
            return List.of();
        }

        return mapSupermarkets(root, city, countryName);
    }

    private String buildOverpassQuery(String city, CityContext cityContext) {
        if (cityContext != null && isNumeric(cityContext.latitude()) && isNumeric(cityContext.longitude())) {
            return """
                    [out:json][timeout:25];
                    (
                      node["shop"="supermarket"](around:%d,%s,%s);
                      node["shop"="hypermarket"](around:%d,%s,%s);
                      way["shop"="supermarket"](around:%d,%s,%s);
                      way["shop"="hypermarket"](around:%d,%s,%s);
                      relation["shop"="supermarket"](around:%d,%s,%s);
                      relation["shop"="hypermarket"](around:%d,%s,%s);
                    );
                    out tags center %d;
                    """.formatted(
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    searchRadiusMeters, cityContext.latitude(), cityContext.longitude(),
                    maxSupermarkets * fetchMultiplier
            );
        }

        return """
                [out:json][timeout:25];
                area[\"name\"=\"%s\"][\"boundary\"=\"administrative\"]->.searchArea;
                (
                  node[\"shop\"=\"supermarket\"](area.searchArea);
                  node[\"shop\"=\"hypermarket\"](area.searchArea);
                  way[\"shop\"=\"supermarket\"](area.searchArea);
                  way[\"shop\"=\"hypermarket\"](area.searchArea);
                  relation[\"shop\"=\"supermarket\"](area.searchArea);
                  relation[\"shop\"=\"hypermarket\"](area.searchArea);
                );
                out tags center %d;
                """.formatted(escapeOverpassValue(city), maxSupermarkets * fetchMultiplier);
    }

    private List<SupermarketDTO> mapSupermarkets(JsonNode root, String city, String countryName) {
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

        }

        return unique.values().stream()
                .sorted(Comparator.comparingInt(this::priorityScore)
                        .thenComparing(SupermarketDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(maxSupermarkets)
                .toList();
    }

    private int priorityScore(SupermarketDTO supermarket) {
        String name = supermarket.getName() == null ? "" : supermarket.getName().toLowerCase();

        if (name.contains("big c")) {
            return 0;
        }
        if (name.contains("lotus") || name.contains("tesco")) {
            return 1;
        }
        if (name.contains("tops") || name.contains("villa") || name.contains("foodland") || name.contains("makro")) {
            return 2;
        }
        if (name.contains("supermarket") || name.contains("hypermarket") || name.contains("market")) {
            return 3;
        }
        return 4;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
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

    private JsonNode executeOverpassQuery(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("User-Agent", userAgent);

            ResponseEntity<String> response = restTemplate.exchange(
                    OVERPASS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(query, headers),
                    String.class
            );

            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.warn("Discovery API call failed for Overpass query {}: {}", query, e.getMessage());
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

    public record CityContext(String city, String countryName, String countryCode, String latitude, String longitude) {
    }
}
