package com.chef.william.repository;

import com.chef.william.model.IngredientStoreListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IngredientStoreListingRepository extends JpaRepository<IngredientStoreListing, Long> {

    List<IngredientStoreListing> findByIngredientIdAndExpiresAtAfterOrderByDistanceKmAsc(
            Long ingredientId,
            LocalDateTime asOf);
}
