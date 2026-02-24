package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "city_supermarket",
        indexes = {
                @Index(name = "idx_city_supermarket_city", columnList = "city")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitySupermarket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 180)
    private String supermarketName;

    @Column(length = 500)
    private String officialWebsite;

    @Column(length = 500)
    private String catalogSearchUrl;



    @Column(length = 120)
    private String normalizedCity;

    @Column(length = 180)
    private String normalizedSupermarketName;

    @Column(length = 255)
    private String canonicalDomain;

    @Column(length = 40)
    private String sourceProvider;

    private java.time.LocalDateTime lastDiscoveryAt;

    private java.time.LocalDateTime lastVerifiedAt;

    @Column(length = 30)
    private String verificationStatus;

    private Double verificationConfidence;

    private Double websiteResolverConfidence;

    @Column(length = 255)
    private String lastFailureReason;

    private java.time.LocalDateTime ttlExpiresAt;

    @Column(length = 500)
    private String notes;
}
