// File: backend/src/test/java/com/app/portfolio/repository/AssetRepositoryTest.java
package com.app.portfolio.repository;

import com.app.portfolio.beans.Asset;
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
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Asset Repository Tests")
class AssetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssetRepository assetRepository;

    private User testUser;
    private Client testClient;
    private Asset testAsset1;
    private Asset testAsset2;
    private Asset testAsset3;

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
                .phone("1234567890")
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset1 = Asset.builder()
                .client(testClient)
                .name("Apple Stock")
                .category(Asset.AssetCategory.STOCK)
                .symbol("AAPL")
                .quantity(new BigDecimal("100"))
                .buyingRate(new BigDecimal("150.00"))
                .purchaseDate(LocalDate.now().minusDays(30))
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset2 = Asset.builder()
                .client(testClient)
                .name("Google Stock")
                .category(Asset.AssetCategory.STOCK)
                .symbol("GOOGL")
                .quantity(new BigDecimal("50"))
                .buyingRate(new BigDecimal("2500.00"))
                .purchaseDate(LocalDate.now().minusDays(15))
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset3 = Asset.builder()
                .client(testClient)
                .name("Bitcoin")
                .category(Asset.AssetCategory.CRYPTO)
                .symbol("BTC")
                .quantity(new BigDecimal("1"))
                .buyingRate(new BigDecimal("45000.00"))
                .purchaseDate(LocalDate.now().minusDays(5))
                .currency("USD")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        entityManager.persist(testUser);
        entityManager.persist(testClient);
        entityManager.persist(testAsset1);
        entityManager.persist(testAsset2);
        entityManager.persist(testAsset3);
        entityManager.flush();
    }

    @Nested
    @DisplayName("FindByClientIdOrderByPurchaseDateDesc Tests")
    class FindByClientIdOrderByPurchaseDateDescTests {

        @Test
        @DisplayName("Should return assets ordered by purchase date descending")
        void shouldReturnAssetsOrderedByPurchaseDateDesc() {
            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateDesc(testClient.getId());

            assertThat(assets).hasSize(3);
            assertThat(assets.get(0).getName()).isEqualTo("Bitcoin");
            assertThat(assets.get(1).getName()).isEqualTo("Google Stock");
            assertThat(assets.get(2).getName()).isEqualTo("Apple Stock");
        }

        @Test
        @DisplayName("Should return empty list for non-existent client ID")
        void shouldReturnEmptyListForNonExistentClientId() {
            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateDesc(999L);

            assertThat(assets).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when client has no assets")
        void shouldReturnEmptyListWhenClientHasNoAssets() {
            User newUser = User.builder()
                    .name("New User")
                    .email("newuser@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Client newClient = Client.builder()
                    .user(newUser)
                    .name("New Client")
                    .email("newclient@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            entityManager.persist(newUser);
            entityManager.persist(newClient);
            entityManager.flush();

            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateDesc(newClient.getId());

            assertThat(assets).isEmpty();
        }
    }

    @Nested
    @DisplayName("ExistsByIdAndClientUserId Tests")
    class ExistsByIdAndClientUserIdTests {

        @Test
        @DisplayName("Should return true when asset exists and belongs to user's client")
        void shouldReturnTrueWhenAssetExistsAndBelongsToUser() {
            boolean exists = assetRepository.existsByIdAndClientUserId(testAsset1.getId(), testUser.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when asset exists but belongs to different user")
        void shouldReturnFalseWhenAssetExistsButBelongsToDifferentUser() {
            boolean exists = assetRepository.existsByIdAndClientUserId(testAsset1.getId(), 999L);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when asset does not exist")
        void shouldReturnFalseWhenAssetDoesNotExist() {
            boolean exists = assetRepository.existsByIdAndClientUserId(999L, testUser.getId());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Standard JpaRepository Tests")
    class StandardJpaRepositoryTests {

        @Test
        @DisplayName("Should save asset successfully")
        void shouldSaveAssetSuccessfully() {
            Asset newAsset = Asset.builder()
                    .client(testClient)
                    .name("Microsoft Stock")
                    .category(Asset.AssetCategory.STOCK)
                    .symbol("MSFT")
                    .quantity(new BigDecimal("75"))
                    .buyingRate(new BigDecimal("300.00"))
                    .purchaseDate(LocalDate.now())
                    .currency("USD")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Asset savedAsset = assetRepository.save(newAsset);

            assertThat(savedAsset.getId()).isNotNull();
            assertThat(savedAsset.getName()).isEqualTo("Microsoft Stock");
            assertThat(savedAsset.getSymbol()).isEqualTo("MSFT");
        }

        @Test
        @DisplayName("Should find asset by ID")
        void shouldFindAssetById() {
            Optional<Asset> foundAsset = assetRepository.findById(testAsset1.getId());

            assertThat(foundAsset).isPresent();
            assertThat(foundAsset.get().getName()).isEqualTo("Apple Stock");
        }

        @Test
        @DisplayName("Should return empty when asset not found by ID")
        void shouldReturnEmptyWhenAssetNotFoundById() {
            Optional<Asset> foundAsset = assetRepository.findById(999L);

            assertThat(foundAsset).isEmpty();
        }

        @Test
        @DisplayName("Should delete asset successfully")
        void shouldDeleteAssetSuccessfully() {
            assetRepository.delete(testAsset1);
            entityManager.flush();

            Optional<Asset> deletedAsset = assetRepository.findById(testAsset1.getId());

            assertThat(deletedAsset).isEmpty();
        }

        @Test
        @DisplayName("Should count all assets")
        void shouldCountAllAssets() {
            long count = assetRepository.count();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all assets")
        void shouldFindAllAssets() {
            List<Asset> allAssets = assetRepository.findAll();

            assertThat(allAssets).hasSize(3);
            assertThat(allAssets).extracting(Asset::getName)
                    .containsExactlyInAnyOrder("Apple Stock", "Google Stock", "Bitcoin");
        }
    }
}
