package com.chef.william.service.ingredient.discovery;

import com.chef.william.dto.discovery.SupermarketDTO;
import com.chef.william.dto.discovery.SupermarketDiscoveryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        return SupermarketDiscoveryResponseDTO.builder()
                .ingredientName(ingredientName)
                .city(city)
                .country(cityContext.countryName())
                .fallbackUsed(fallbackUsed)
                .supermarkets(supermarkets)
                .build();
    }
}
