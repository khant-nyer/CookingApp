package com.chef.william.service.ingredient;

import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.service.discovery.SupermarketDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientDiscoveryFacade {

    private final SupermarketDiscoveryService supermarketDiscoveryService;

    @Transactional
    public List<SupermarketDiscoveryDTO> discoverPopularSupermarkets(Long userId, String city, String ingredientName) {
        return supermarketDiscoveryService.discover(userId, city, ingredientName);
    }
}
