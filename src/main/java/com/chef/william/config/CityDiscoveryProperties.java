package com.chef.william.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.city-discovery")
public class CityDiscoveryProperties {

    private String provider = "osm";
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org/search";
    private int maxCandidates = 10;
    private long cacheTtlSeconds = 1800;
    private long minRequestIntervalMs = 1000;
    private int maxRetries = 2;
    private long retryBackoffMs = 700;
    private double minConfidenceToPersist = 0.55;
    private long marketCacheTtlSeconds = 21600;
    private boolean searchProviderEnabled = true;
    private String searchProviderBaseUrl = "https://duckduckgo.com/html/";
}
