package com.chef.william.service.discovery;

import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.model.CitySupermarket;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.service.crawler.SupermarketCrawlerClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cookingapp;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SupermarketDiscoveryServiceIntegrationTest {

    @Autowired
    private SupermarketDiscoveryService discoveryService;

    @Autowired
    private CitySupermarketRepository citySupermarketRepository;

    @MockBean
    private SupermarketCrawlerClient supermarketCrawlerClient;

    @Test
    void fallbackDiscoveryShouldPersistMatchedAndReuseDbOnSecondCall() {
        citySupermarketRepository.deleteAll();

        when(supermarketCrawlerClient.webpageContainsIngredient(anyString(), eq("Soy Sauce")))
                .thenReturn(true, false, false, true);

        List<SupermarketDiscoveryDTO> first = discoveryService.discover(null, "Bangkok", "Soy Sauce");

        assertEquals(3, first.size());
        assertTrue(first.stream().anyMatch(SupermarketDiscoveryDTO::isIngredientMatched));

        List<CitySupermarket> persisted = citySupermarketRepository.findByCityIgnoreCase("Bangkok");
        assertEquals(1, persisted.size());
        assertEquals("Big C", persisted.get(0).getSupermarketName());

        List<SupermarketDiscoveryDTO> second = discoveryService.discover(null, "Bangkok", "Soy Sauce");
        assertEquals(1, second.size());
        assertEquals("Big C", second.get(0).getSupermarketName());
        assertEquals("DB", second.get(0).getDiscoverySource());

        verify(supermarketCrawlerClient, times(4)).webpageContainsIngredient(anyString(), eq("Soy Sauce"));
    }
}
