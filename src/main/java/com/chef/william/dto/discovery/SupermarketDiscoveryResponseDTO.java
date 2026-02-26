package com.chef.william.dto.discovery;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SupermarketDiscoveryResponseDTO {
    String ingredientName;
    String city;
    String country;
    boolean fallbackUsed;
    List<SupermarketDTO> supermarkets;
}
