package com.chef.william.repository;

import com.chef.william.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    int countByFoodId(Long foodId);

    boolean existsByFoodId(Long foodId);

    List<Recipe> findByFoodId(Long foodId);

    boolean existsByVersion(String version);

    @Query("SELECT r.food.id as foodId, COUNT(r) as recipeCount FROM Recipe r WHERE r.food.id IN :foodIds GROUP BY r.food.id")
    List<FoodRecipeCountProjection> countByFoodIds(@Param("foodIds") Collection<Long> foodIds);
}
