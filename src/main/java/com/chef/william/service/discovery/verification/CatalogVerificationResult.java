package com.chef.william.service.discovery.verification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogVerificationResult {
    private boolean matched;
    private String matchSource;
}
