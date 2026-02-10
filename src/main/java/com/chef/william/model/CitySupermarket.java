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

    @Column(length = 500)
    private String notes;
}
