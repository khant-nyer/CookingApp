package com.chef.william.service.discovery.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityDiscoveryCandidate {
    private String supermarketName;
    private String website;
    private double sourceConfidence;
}
