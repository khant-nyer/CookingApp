package com.chef.william.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientStoreListingDTO {
    private Long id;
    private String storeName;
    private String storeAddress;
    private String storePlaceId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal price;
    private String currency;
    private Boolean inStock;
    private BigDecimal distanceKm;
    private String sourceProvider;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;

}
