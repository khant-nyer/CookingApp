package com.chef.william.service.discovery;

import com.chef.william.dto.SupermarketDiscoveryDTO;
import com.chef.william.exception.BusinessException;
import com.chef.william.model.CitySupermarket;
import com.chef.william.repository.CitySupermarketRepository;
import com.chef.william.service.crawler.SupermarketCrawlerClient;
import com.chef.william.service.discovery.provider.CityDiscoveryCandidate;
import com.chef.william.service.discovery.provider.CityDiscoveryProvider;
import com.chef.william.service.discovery.verification.CatalogVerificationResult;
import com.chef.william.service.discovery.verification.SupermarketCatalogVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @MockBean
    private CityDiscoveryProvider cityDiscoveryProvider;

    @MockBean
    private SupermarketCatalogVerifier supermarketCatalogVerifier;

    @Test
    void dbDiscoveryShouldReturnOnlyCrawlVerifiedMatchesForPersistedCitySupermarkets() {
        citySupermarketRepository.deleteAll();
        when(cityDiscoveryProvider.discoverSupermarkets(anyString())).thenReturn(List.of());

        CitySupermarket bigC = new CitySupermarket();
        bigC.setCity("Bangkok");
        bigC.setSupermarketName("Big C");
        bigC.setOfficialWebsite("https://www.bigc.co.th");
        bigC.setCatalogSearchUrl("https://www.bigc.co.th/search?q={ingredient}");

        CitySupermarket lotuss = new CitySupermarket();
        lotuss.setCity("Bangkok");
        lotuss.setSupermarketName("Lotus's");
        lotuss.setOfficialWebsite("https://www.lotuss.com");
        lotuss.setCatalogSearchUrl("https://www.lotuss.com/th/search/{ingredient}");

        citySupermarketRepository.saveAll(List.of(bigC, lotuss));

        when(supermarketCatalogVerifier.verifyIngredient(anyString(), eq("Soy Sauce")))
                .thenReturn(new CatalogVerificationResult(true, "STRUCTURED_PRODUCT_SCRAPE"),
                        new CatalogVerificationResult(true, "OFFICIAL_WEB_CRAWL"));

        List<SupermarketDiscoveryDTO> first = discoveryService.discover("Bangkok", "Soy Sauce");

        assertEquals(2, first.size());
        assertTrue(first.stream().allMatch(dto -> dto.getDiscoverySource().equals("DB_CACHE")));
        assertTrue(first.stream().anyMatch(dto -> dto.getMatchSource().equals("STRUCTURED_PRODUCT_SCRAPE")));

        verify(supermarketCatalogVerifier, times(2)).verifyIngredient(anyString(), eq("Soy Sauce"));
    }

    @Test
    void discoveryShouldFailForCityWithoutVerifiedSupermarkets() {
        citySupermarketRepository.deleteAll();
        when(cityDiscoveryProvider.discoverSupermarkets("Manila")).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> discoveryService.discover("Manila", "Sunflower Oil"));

        assertTrue(ex.getMessage().contains("No verified supermarkets found for city: Manila"));
    }

    @Test
    void discoveryShouldFailWhenNoPersistedMarketCrawlMatchesIngredient() {
        citySupermarketRepository.deleteAll();

        CitySupermarket bigC = new CitySupermarket();
        bigC.setCity("Bangkok");
        bigC.setSupermarketName("Big C");
        bigC.setOfficialWebsite("https://www.bigc.co.th");
        bigC.setCatalogSearchUrl("https://www.bigc.co.th/search?q={ingredient}");
        citySupermarketRepository.save(bigC);

        when(supermarketCatalogVerifier.verifyIngredient(anyString(), eq("Beef")))
                .thenReturn(new CatalogVerificationResult(false, "NO_MATCH_ON_CRAWL"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> discoveryService.discover("Bangkok", "Beef"));

        assertTrue(ex.getMessage().contains("No verified supermarket matches found"));
    }

    @Test
    void discoveryShouldNotFallbackToBangkokRowsForUnknownCity() {
        citySupermarketRepository.deleteAll();

        CitySupermarket bangkokOnly = new CitySupermarket();
        bangkokOnly.setCity("Bangkok");
        bangkokOnly.setSupermarketName("Big C");
        bangkokOnly.setOfficialWebsite("https://www.bigc.co.th");
        bangkokOnly.setCatalogSearchUrl("https://www.bigc.co.th/search?q={ingredient}");
        citySupermarketRepository.save(bangkokOnly);

        when(cityDiscoveryProvider.discoverSupermarkets("Paris")).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> discoveryService.discover("Paris", "Rice"));

        assertTrue(ex.getMessage().contains("No verified supermarkets found for city: Paris"));
        assertTrue(citySupermarketRepository.findByCityIgnoreCase("Paris").isEmpty());
    }



    @Test
    void discoveryShouldReturnTemplateBackedFallbackMarketWhenCrawlVerificationFails() {
        citySupermarketRepository.deleteAll();

        when(cityDiscoveryProvider.discoverSupermarkets("London")).thenReturn(List.of());
        when(supermarketCatalogVerifier.verifyIngredient(anyString(), eq("Soy Sauce")))
                .thenReturn(new CatalogVerificationResult(false, "NO_MATCH_ON_CRAWL"));

        List<SupermarketDiscoveryDTO> result = discoveryService.discover("London", "Soy Sauce");

        assertTrue(result.stream().anyMatch(dto -> dto.getSupermarketName().equals("Sainsbury's")));
        assertTrue(result.stream().anyMatch(dto -> dto.getMatchSource().equals("CATALOG_SEARCH_URL_TEMPLATE")));
    }

    @Test
    void discoveryShouldUseConfiguredLondonFallbackMarketsWhenProviderHasNoCandidates() {
        citySupermarketRepository.deleteAll();

        when(cityDiscoveryProvider.discoverSupermarkets("London")).thenReturn(List.of());
        when(supermarketCatalogVerifier.verifyIngredient(anyString(), eq("Soy Sauce")))
                .thenReturn(new CatalogVerificationResult(true, "STRUCTURED_PRODUCT_SCRAPE"));

        List<SupermarketDiscoveryDTO> result = discoveryService.discover("London", "Soy Sauce");

        assertTrue(result.size() >= 1);
        assertTrue(result.stream().anyMatch(dto -> dto.getSupermarketName().equals("Sainsbury's")));
        assertTrue(result.stream().allMatch(dto -> dto.getCity().equals("London")));
        assertTrue(result.stream().allMatch(dto -> dto.getDiscoverySource().equals("DB_CACHE")));
        assertTrue(citySupermarketRepository.existsByCityIgnoreCaseAndSupermarketNameIgnoreCase("London", "Sainsbury's"));
    }

    @Test
    void discoveryShouldPersistOnlyReachableProviderCandidates() {
        citySupermarketRepository.deleteAll();

        when(cityDiscoveryProvider.discoverSupermarkets("Yangon"))
                .thenReturn(List.of(new CityDiscoveryCandidate(
                        "City Mart",
                        "https://www.citymart.com.mm/search/{ingredient}",
                        0.82
                )));
        when(supermarketCatalogVerifier.verifyIngredient(anyString(), eq("Soy Sauce")))
                .thenReturn(new CatalogVerificationResult(true, "STRUCTURED_PRODUCT_SCRAPE"));

        List<SupermarketDiscoveryDTO> result = discoveryService.discover("Yangon", "Soy Sauce");

        assertEquals(1, result.size());
        assertEquals("Yangon", result.get(0).getCity());
        assertEquals("City Mart", result.get(0).getSupermarketName());
        assertEquals("DB_CACHE", result.get(0).getDiscoverySource());
        assertTrue(citySupermarketRepository.existsByCityIgnoreCaseAndSupermarketNameIgnoreCase("Yangon", "City Mart"));
    }

    @Test
    void discoveryShouldNotPersistUnreachableProviderCandidates() {
        citySupermarketRepository.deleteAll();

        when(cityDiscoveryProvider.discoverSupermarkets("Oslo")).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> discoveryService.discover("Oslo", "Rice"));

        assertTrue(ex.getMessage().contains("No verified supermarkets found for city: Oslo"));
        assertTrue(citySupermarketRepository.findByCityIgnoreCase("Oslo").isEmpty());
    }

}
