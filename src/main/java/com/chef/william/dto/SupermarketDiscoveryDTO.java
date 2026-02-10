package com.chef.william.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupermarketDiscoveryDTO {
    private String city;
    private String supermarketName;
    private String officialWebsite;
    private String catalogSearchUrl;
    private boolean ingredientMatched;
    private String matchSource;
    private String discoverySource;
    private LocalDateTime checkedAt;
}
