// File: backend/src/test/java/com/app/portfolio/repository/AssetPriceRepositoryTest.java
package com.app.portfolio.repository;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Asset Price Repository Tests")
class AssetPriceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssetPriceRepository assetPriceRepository;

    private User testUser;
    private Client testClient;
    private Asset testAsset;
    private AssetPrice oldPrice;
    private AssetPrice recentPrice;
    private AssetPrice latestPrice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testClient = Client.builder()
                .user(testUser)
                .name("Test Client")
                .email("client@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset = Asset.builder()
                .client(testClient)
                .name("Test Asset")
                .category(Asset.AssetCategory.STOCK)
                .symbol("TEST")
                .quantity(new BigDecimal("100"))
                .buyingRate(new BigDecimal("100.00"))
                .purchaseDate(java.time.LocalDate.now())
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant now = Instant.now();

        oldPrice = AssetPrice.builder()
                .asset(testAsset)
                .currentPrice(new BigDecimal("95.50"))
                .priceDate(threeDaysAgo)
                .source(AssetPrice.PriceSource.YFINANCE)
                .build();

        recentPrice = AssetPrice.builder()
                .asset(testAsset)
                .currentPrice(new BigDecimal("98.75"))
                .priceDate(oneDayAgo)
                .source(AssetPrice.PriceSource.MANUAL)
                .build();

        latestPrice = AssetPrice.builder()
                .asset(testAsset)
                .currentPrice(new BigDecimal("102.25"))
                .priceDate(now)
                .source(AssetPrice.PriceSource.FAKE)
                .build();

        entityManager.persist(testUser);
        entityManager.persist(testClient);
        entityManager.persist(testAsset);
        entityManager.persist(oldPrice);
        entityManager.persist(recentPrice);
        entityManager.persist(latestPrice);
        entityManager.flush();
    }

    @Nested
    @DisplayName("FindFirstByAssetIdOrderByPriceDateDesc Tests")
    class FindFirstByAssetIdOrderByPriceDateDescTests {

        @Test
        @DisplayName("Should return latest price for asset")
        void shouldReturnLatestPriceForAsset() {
            Optional<AssetPrice> foundPrice = assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(testAsset.getId());

            assertThat(foundPrice).isPresent();
            assertThat(foundPrice.get().getCurrentPrice()).isEqualTo(new BigDecimal("102.25"));
            assertThat(foundPrice.get().getSource()).isEqualTo(AssetPrice.PriceSource.FAKE);
        }

        @Test
        @DisplayName("Should return empty when asset has no prices")
        void shouldReturnEmptyWhenAssetHasNoPrices() {
            Asset newAsset = Asset.builder()
                    .client(testClient)
                    .name("New Asset")
                    .category(Asset.AssetCategory.STOCK)
                    .symbol("NEW")
                    .quantity(new BigDecimal("50"))
                    .buyingRate(new BigDecimal("50.00"))
                    .purchaseDate(java.time.LocalDate.now())
                    .currency("USD")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            entityManager.persist(newAsset);
            entityManager.flush();

            Optional<AssetPrice> foundPrice = assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(newAsset.getId());

            assertThat(foundPrice).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when asset does not exist")
        void shouldReturnEmptyWhenAssetDoesNotExist() {
            Optional<AssetPrice> foundPrice = assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(999L);

            assertThat(foundPrice).isEmpty();
        }

        @Test
        @DisplayName("Should handle single price correctly")
        void shouldHandleSinglePriceCorrectly() {
            Asset singlePriceAsset = Asset.builder()
                    .client(testClient)
                    .name("Single Price Asset")
                    .category(Asset.AssetCategory.STOCK)
                    .symbol("SINGLE")
                    .quantity(new BigDecimal("25"))
                    .buyingRate(new BigDecimal("25.00"))
                    .purchaseDate(java.time.LocalDate.now())
                    .currency("USD")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            entityManager.persist(singlePriceAsset);

            AssetPrice singlePrice = AssetPrice.builder()
                    .asset(singlePriceAsset)
                    .currentPrice(new BigDecimal("30.00"))
                    .priceDate(Instant.now())
                    .source(AssetPrice.PriceSource.MANUAL)
                    .build();

            entityManager.persist(singlePrice);
            entityManager.flush();

            Optional<AssetPrice> foundPrice = assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(singlePriceAsset.getId());

            assertThat(foundPrice).isPresent();
            assertThat(foundPrice.get().getCurrentPrice()).isEqualTo(new BigDecimal("30.00"));
        }
    }

    @Nested
    @DisplayName("Standard JpaRepository Tests")
    class StandardJpaRepositoryTests {

        @Test
        @DisplayName("Should save asset price successfully")
        void shouldSaveAssetPriceSuccessfully() {
            AssetPrice newPrice = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("105.50"))
                    .priceDate(Instant.now().plusSeconds(3600))
                    .source(AssetPrice.PriceSource.YFINANCE)
                    .build();

            AssetPrice savedPrice = assetPriceRepository.save(newPrice);

            assertThat(savedPrice.getId()).isNotNull();
            assertThat(savedPrice.getCurrentPrice()).isEqualTo(new BigDecimal("105.50"));
            assertThat(savedPrice.getSource()).isEqualTo(AssetPrice.PriceSource.YFINANCE);
        }

        @Test
        @DisplayName("Should find asset price by ID")
        void shouldFindAssetPriceById() {
            Optional<AssetPrice> foundPrice = assetPriceRepository.findById(latestPrice.getId());

            assertThat(foundPrice).isPresent();
            assertThat(foundPrice.get().getCurrentPrice()).isEqualTo(new BigDecimal("102.25"));
        }

        @Test
        @DisplayName("Should return empty when asset price not found by ID")
        void shouldReturnEmptyWhenAssetPriceNotFoundById() {
            Optional<AssetPrice> foundPrice = assetPriceRepository.findById(999L);

            assertThat(foundPrice).isEmpty();
        }

        @Test
        @DisplayName("Should delete asset price successfully")
        void shouldDeleteAssetPriceSuccessfully() {
            assetPriceRepository.delete(oldPrice);
            entityManager.flush();

            Optional<AssetPrice> deletedPrice = assetPriceRepository.findById(oldPrice.getId());

            assertThat(deletedPrice).isEmpty();
        }

        @Test
        @DisplayName("Should count all asset prices")
        void shouldCountAllAssetPrices() {
            long count = assetPriceRepository.count();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all asset prices")
        void shouldFindAllAssetPrices() {
            List<AssetPrice> allPrices = assetPriceRepository.findAll();

            assertThat(allPrices).hasSize(3);
            assertThat(allPrices).extracting(AssetPrice::getCurrentPrice)
                    .containsExactlyInAnyOrder(
                            new BigDecimal("95.50"),
                            new BigDecimal("98.75"),
                            new BigDecimal("102.25")
                    );
        }

        @Test
        @DisplayName("Should update asset price successfully")
        void shouldUpdateAssetPriceSuccessfully() {
            latestPrice.setCurrentPrice(new BigDecimal("103.00"));
            latestPrice.setSource(AssetPrice.PriceSource.MANUAL);

            AssetPrice updatedPrice = assetPriceRepository.save(latestPrice);

            assertThat(updatedPrice.getCurrentPrice()).isEqualTo(new BigDecimal("103.00"));
            assertThat(updatedPrice.getSource()).isEqualTo(AssetPrice.PriceSource.MANUAL);
        }
    }

    @Nested
    @DisplayName("Price Source Tests")
    class PriceSourceTests {

        @Test
        @DisplayName("Should save all price source types")
        void shouldSaveAllPriceSourceTypes() {
            AssetPrice yfinancePrice = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("100.00"))
                    .priceDate(Instant.now())
                    .source(AssetPrice.PriceSource.YFINANCE)
                    .build();

            AssetPrice manualPrice = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("101.00"))
                    .priceDate(Instant.now())
                    .source(AssetPrice.PriceSource.MANUAL)
                    .build();

            AssetPrice fakePrice = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("102.00"))
                    .priceDate(Instant.now())
                    .source(AssetPrice.PriceSource.FAKE)
                    .build();

            entityManager.persist(yfinancePrice);
            entityManager.persist(manualPrice);
            entityManager.persist(fakePrice);
            entityManager.flush();

            List<AssetPrice> allPrices = assetPriceRepository.findAll();

            assertThat(allPrices).hasSize(6);
            assertThat(allPrices).extracting(AssetPrice::getSource)
                    .containsExactlyInAnyOrder(
                            AssetPrice.PriceSource.YFINANCE,
                            AssetPrice.PriceSource.MANUAL,
                            AssetPrice.PriceSource.FAKE,
                            AssetPrice.PriceSource.YFINANCE,
                            AssetPrice.PriceSource.MANUAL,
                            AssetPrice.PriceSource.FAKE
                    );
        }
    }

    @Nested
    @DisplayName("Entity Relationship Tests")
    class EntityRelationshipTests {

        @Test
        @DisplayName("Should maintain relationship with asset")
        void shouldMaintainRelationshipWithAsset() {
            Optional<AssetPrice> foundPrice = assetPriceRepository.findById(latestPrice.getId());

            assertThat(foundPrice).isPresent();
            assertThat(foundPrice.get().getAsset().getId()).isEqualTo(testAsset.getId());
            assertThat(foundPrice.get().getAsset().getSymbol()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("Should handle cascade operations correctly")
        void shouldHandleCascadeOperationsCorrectly() {
            Asset newAsset = Asset.builder()
                    .client(testClient)
                    .name("Cascade Test Asset")
                    .category(Asset.AssetCategory.STOCK)
                    .symbol("CASCADE")
                    .quantity(new BigDecimal("10"))
                    .buyingRate(new BigDecimal("10.00"))
                    .purchaseDate(java.time.LocalDate.now())
                    .currency("USD")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            entityManager.persist(newAsset);

            AssetPrice newPrice = AssetPrice.builder()
                    .asset(newAsset)
                    .currentPrice(new BigDecimal("15.00"))
                    .priceDate(Instant.now())
                    .source(AssetPrice.PriceSource.MANUAL)
                    .build();

            AssetPrice savedPrice = assetPriceRepository.save(newPrice);

            entityManager.flush();
            entityManager.clear();

            Optional<AssetPrice> retrievedPrice = assetPriceRepository.findById(savedPrice.getId());

            assertThat(retrievedPrice).isPresent();
            assertThat(retrievedPrice.get().getAsset()).isNotNull();
            assertThat(retrievedPrice.get().getAsset().getId()).isEqualTo(newAsset.getId());
        }
    }

    @Nested
    @DisplayName("Price Ordering Tests")
    class PriceOrderingTests {

        @Test
        @DisplayName("Should return prices in correct chronological order")
        void shouldReturnPricesInCorrectChronologicalOrder() {
            List<AssetPrice> allPrices = assetPriceRepository.findAll();

            assertThat(allPrices).hasSize(3);
            
            AssetPrice earliest = allPrices.stream()
                    .filter(p -> p.getCurrentPrice().equals(new BigDecimal("95.50")))
                    .findFirst()
                    .orElseThrow();
            
            AssetPrice middle = allPrices.stream()
                    .filter(p -> p.getCurrentPrice().equals(new BigDecimal("98.75")))
                    .findFirst()
                    .orElseThrow();
            
            AssetPrice latest = allPrices.stream()
                    .filter(p -> p.getCurrentPrice().equals(new BigDecimal("102.25")))
                    .findFirst()
                    .orElseThrow();

            assertThat(earliest.getPriceDate()).isBefore(middle.getPriceDate());
            assertThat(middle.getPriceDate()).isBefore(latest.getPriceDate());
        }

        @Test
        @DisplayName("Should handle same timestamp prices correctly")
        void shouldHandleSameTimestampPricesCorrectly() {
            Instant sameTime = Instant.now();

            AssetPrice price1 = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("100.00"))
                    .priceDate(sameTime)
                    .source(AssetPrice.PriceSource.YFINANCE)
                    .build();

            AssetPrice price2 = AssetPrice.builder()
                    .asset(testAsset)
                    .currentPrice(new BigDecimal("101.00"))
                    .priceDate(sameTime)
                    .source(AssetPrice.PriceSource.MANUAL)
                    .build();

            entityManager.persist(price1);
            entityManager.persist(price2);
            entityManager.flush();

            Optional<AssetPrice> foundPrice = assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(testAsset.getId());

            assertThat(foundPrice).isPresent();
            assertThat(foundPrice.get().getCurrentPrice()).isIn(new BigDecimal("100.00"), new BigDecimal("101.00"));
        }
    }
}
