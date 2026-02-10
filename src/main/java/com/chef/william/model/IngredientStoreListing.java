package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_store_listing",
        indexes = {
                @Index(name = "idx_listing_ingredient", columnList = "ingredient_id"),
                @Index(name = "idx_listing_expires_at", columnList = "expires_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientStoreListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, length = 200)
    private String storeName;

    @Column(length = 500)
    private String storeAddress;

    @Column(name = "store_place_id", length = 120)
    private String storePlaceId;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency;

    @Column(nullable = false)
    private Boolean inStock = Boolean.TRUE;

    @Column(name = "distance_km", precision = 7, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "source_provider", length = 80)
    private String sourceProvider;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
