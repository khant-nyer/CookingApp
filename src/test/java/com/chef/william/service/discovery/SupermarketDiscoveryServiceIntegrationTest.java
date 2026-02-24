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
                .thenReturn(false, false, false, true, false, false);

        List<SupermarketDiscoveryDTO> first = discoveryService.discover("Bangkok", "Soy Sauce");

        assertEquals(3, first.size());
        assertTrue(first.stream().anyMatch(SupermarketDiscoveryDTO::isIngredientMatched));
        assertTrue(first.stream().allMatch(dto -> dto.getMatchSource().equals("CATALOG_URL_QUERY_MATCH")));

        List<CitySupermarket> persisted = citySupermarketRepository.findByCityIgnoreCase("Bangkok");
        assertEquals(3, persisted.size());

        List<SupermarketDiscoveryDTO> second = discoveryService.discover("Bangkok", "Soy Sauce");
        assertEquals(3, second.size());
        assertTrue(second.stream().allMatch(dto -> dto.getDiscoverySource().equals("DB")));
        assertTrue(second.stream().anyMatch(dto -> dto.getMatchSource().equals("OFFICIAL_WEB_CRAWL")));

        verify(supermarketCrawlerClient, times(6)).webpageContainsIngredient(anyString(), eq("Soy Sauce"));
    }

    @Test
    void fallbackDiscoveryShouldUseGenericSeedsWhenCityIsNotConfigured() {
        citySupermarketRepository.deleteAll();

        when(supermarketCrawlerClient.webpageContainsIngredient(anyString(), eq("Sunflower Oil")))
                .thenReturn(false, false, false);

        List<SupermarketDiscoveryDTO> results = discoveryService.discover("Manila", "Sunflower Oil");

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(SupermarketDiscoveryDTO::isIngredientMatched));
        assertTrue(results.stream().allMatch(dto -> dto.getCity().equals("Manila")));
        assertTrue(results.stream().allMatch(dto -> dto.getDiscoverySource().equals("FALLBACK")));

        List<CitySupermarket> persisted = citySupermarketRepository.findByCityIgnoreCase("Manila");
        assertEquals(3, persisted.size());

        verify(supermarketCrawlerClient, times(3)).webpageContainsIngredient(anyString(), eq("Sunflower Oil"));
    }

}
