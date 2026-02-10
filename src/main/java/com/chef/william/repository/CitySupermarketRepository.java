package com.chef.william.repository;

import com.chef.william.model.CitySupermarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitySupermarketRepository extends JpaRepository<CitySupermarket, Long> {
    List<CitySupermarket> findByCityIgnoreCase(String city);

    boolean existsByCityIgnoreCaseAndSupermarketNameIgnoreCase(String city, String supermarketName);
}
