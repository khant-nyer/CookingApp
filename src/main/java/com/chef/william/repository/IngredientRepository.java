package com.chef.william.repository;

import com.chef.william.model.Ingredient;
import com.chef.william.model.enums.Nutrients;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    // Check if name already exists (case-insensitive) - useful for unique validation
    boolean existsByNameIgnoreCase(String name);

    // Find by exact name (case-insensitive) - for search/autocomplete
    Optional<Ingredient> findByNameIgnoreCase(String name);

    List<Ingredient> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT i FROM Ingredient i JOIN i.nutritionList n " +
            "WHERE n.nutrient = :nutrient AND n.value > :minValue " +
            "ORDER BY n.value DESC")
    List<Ingredient> findByNutrientAndMinValue(
            @Param("nutrient") Nutrients nutrient,
            @Param("minValue") double minValue);

    @Query("SELECT LOWER(i.name) FROM Ingredient i WHERE LOWER(i.name) IN :names")
    Set<String> findExistingNormalizedNames(@Param("names") Collection<String> names);

}
