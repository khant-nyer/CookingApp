package com.chef.william.repository;

import com.chef.william.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    int countByFoodId(Long foodId);

    boolean existsByFoodId(Long foodId);

    List<Recipe> findByFoodId(Long foodId);

    boolean existsByVersion(String version);

    @Query("SELECT r.id FROM Recipe r")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.food WHERE r.id = :id")
    Optional<Recipe> findDetailedById(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.food WHERE r.id IN :ids")
    List<Recipe> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.food WHERE r.food.id = :foodId")
    List<Recipe> findDetailedByFoodId(@Param("foodId") Long foodId);

    @Query("SELECT r.food.id as foodId, COUNT(r) as recipeCount FROM Recipe r WHERE r.food.id IN :foodIds GROUP BY r.food.id")
    List<FoodRecipeCountProjection> countByFoodIds(@Param("foodIds") Collection<Long> foodIds);
}
