package com.chef.william.repository;

import com.chef.william.model.IngredientStoreListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IngredientStoreListingRepository extends JpaRepository<IngredientStoreListing, Long> {

    @Query("""
            SELECT s
            FROM IngredientStoreListing s
            WHERE s.ingredient.id = :ingredientId
              AND (s.expiresAt IS NULL OR s.expiresAt > :asOf)
            ORDER BY s.distanceKm ASC, s.capturedAt DESC
            """)
    List<IngredientStoreListing> findActiveListingsByIngredientId(
            @Param("ingredientId") Long ingredientId,
            @Param("asOf") LocalDateTime asOf);
    List<IngredientStoreListing> findByIngredientIdAndExpiresAtAfterOrderByDistanceKmAsc(
            Long ingredientId,
            LocalDateTime asOf);
}
