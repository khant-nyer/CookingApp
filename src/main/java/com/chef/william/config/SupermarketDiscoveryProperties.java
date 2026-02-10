package com.chef.william.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.supermarket-discovery")
public class SupermarketDiscoveryProperties {

    private List<FallbackMarket> fallbackMarkets = new ArrayList<>();

    @Data
    public static class FallbackMarket {
        private String city;
        private String supermarketName;
        private String officialWebsite;
        private String catalogSearchUrl;
        private String notes;
    }
}
