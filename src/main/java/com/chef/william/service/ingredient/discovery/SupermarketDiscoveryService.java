package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.chef.william.dto.discovery.SupermarketDiscoveryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SupermarketDiscoveryService {

    private final OpenStreetMapSupermarketDiscoveryClient openStreetMapClient;
    private final PlaywrightSupermarketDiscoveryClient playwrightClient;

    public SupermarketDiscoveryResponseDTO discover(String ingredientName, String city) {
        OpenStreetMapSupermarketDiscoveryClient.CityContext cityContext = openStreetMapClient.resolveCity(city)
                .orElse(new OpenStreetMapSupermarketDiscoveryClient.CityContext(city, null, null, null, null));

        List<SupermarketDTO> supermarkets = openStreetMapClient.discoverSupermarkets(
                city,
                cityContext.countryName(),
                cityContext);

        boolean fallbackUsed = false;
        if (supermarkets.isEmpty()) {
            supermarkets = playwrightClient.discoverBySearch(city, cityContext.countryName());
            fallbackUsed = true;
        }

        List<SupermarketDTO> filtered = playwrightClient.filterSupermarketsByIngredient(supermarkets, ingredientName, city);
        if (filtered != null && !filtered.isEmpty()) {
            supermarkets = filtered;
        }
        List<SupermarketDTO> enriched = playwrightClient.enrichSupermarketsMetadata(supermarkets, ingredientName, city);
        if (enriched != null && !enriched.isEmpty()) {
            supermarkets = enriched;
        }
        supermarkets = supermarkets.stream().filter(Objects::nonNull).toList();

        return SupermarketDiscoveryResponseDTO.builder()
                .ingredientName(ingredientName)
                .city(city)
                .country(cityContext.countryName())
                .fallbackUsed(fallbackUsed)
                .supermarkets(supermarkets)
                .build();
    }
}
