package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingredients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String category;

    private String description;

    // Base serving: All nutrition values are defined per this amount/unit
    @Column(nullable = false)
    private Double servingAmount = 100.0;  // e.g., 100.0

    @Column(nullable = false, length = 20)
    private String servingUnit = "g";      // e.g., "g", "ml", "piece" (can change to Unit enum later if needed)

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Nutrition> nutritionList = new ArrayList<>();

    @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY)
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();
}