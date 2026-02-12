package com.chef.william.repository;

import com.chef.william.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    int countByFoodId(Long foodId);

    boolean existsByVersion(String version);
}
