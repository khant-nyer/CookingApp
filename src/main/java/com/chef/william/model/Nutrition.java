package com.chef.william.model;

import com.chef.william.model.enums.Nutrients;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nutrition",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ingredient_id", "nutrient"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nutrients nutrient;

    @Column(nullable = false)
    private Double value;       // Nutrient amount (e.g., 18.0 for calories)

    @Column(nullable = false)
    private String unit;        // Unit of the value (e.g., "kcal", "g", "mg")
}