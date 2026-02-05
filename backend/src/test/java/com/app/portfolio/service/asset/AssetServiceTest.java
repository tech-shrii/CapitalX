package com.app.portfolio.service.asset;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.AssetMapper;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.service.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Asset Service Tests")
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private AssetServiceImpl assetService;

    private User testUser;
    private Client testClient;
    private Asset testAsset1;
    private Asset testAsset2;
    private AssetRequest testAssetRequest;
    private AssetResponse testAssetResponse;

    @BeforeEach
    void setUp() {
        testClient = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@example.com")
                .user(testUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .clients(Arrays.asList(testClient)) // Add client to user's clients list
                .build();

        testAsset1 = Asset.builder()
                .id(1L)
                .symbol("AAPL")
                .quantity(new BigDecimal("100"))
                .buyingRate(new BigDecimal("150.00"))
                .purchaseDate(LocalDate.now())
                .client(testClient)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset2 = Asset.builder()
                .id(2L)
                .symbol("GOOGL")
                .quantity(new BigDecimal("50"))
                .buyingRate(new BigDecimal("2500.00"))
                .purchaseDate(LocalDate.now())
                .client(testClient)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAssetRequest = new AssetRequest();
        testAssetRequest.setSymbol("MSFT");
        testAssetRequest.setQuantity(new BigDecimal("75"));
        testAssetRequest.setBuyingRate(new BigDecimal("300.00"));
        testAssetRequest.setPurchaseDate(LocalDate.now());

        testAssetResponse = AssetResponse.builder()
                .id(1L)
                .symbol("AAPL")
                .quantity(new BigDecimal("100"))
                .buyingRate(new BigDecimal("150.00"))
                .currentPrice(new BigDecimal("155.00"))
                .profitLoss(new BigDecimal("500.00"))
                .profitLossPercent(new BigDecimal("3.33"))
                .purchaseDate(LocalDate.now())
                .build();
    }

    @Nested
    @DisplayName("Get Assets By Client ID Tests")
    class GetAssetsByClientIdTests {

        @Test
        @DisplayName("Should return assets when client exists and belongs to user")
        void shouldReturnAssetsWhenClientExistsAndBelongsToUser() {
            when(clientRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList(testAsset1, testAsset2));
            when(pricingService.getCurrentPrice(1L)).thenReturn(new BigDecimal("155.00"));
            when(pricingService.getCurrentPrice(2L)).thenReturn(new BigDecimal("2600.00"));
            when(assetMapper.toResponse(any(Asset.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(testAssetResponse);

            List<AssetResponse> result = assetService.getAssetsByClientId(1L, 1L);

            assertThat(result).hasSize(2);
            verify(clientRepository).existsByIdAndUserId(1L, 1L);
            verify(assetRepository).findByClientIdOrderByPurchaseDateDesc(1L);
            verify(pricingService, times(2)).getCurrentPrice(anyLong());
            verify(assetMapper, times(2)).toResponse(any(Asset.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientDoesNotExist() {
            when(clientRepository.existsByIdAndUserId(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> assetService.getAssetsByClientId(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client")
                    .hasMessageContaining("1");

            verify(clientRepository).existsByIdAndUserId(1L, 1L);
            verifyNoInteractions(assetRepository, pricingService, assetMapper);
        }
    }

    @Nested
    @DisplayName("Create Asset Tests")
    class CreateAssetTests {

        @Test
        @DisplayName("Should create asset when client exists and belongs to user")
        void shouldCreateAssetWhenClientExistsAndBelongsToUser() {
            // Create a fresh client for this test to avoid circular reference
            Client testClientForCreate = Client.builder()
                    .id(1L)
                    .name("Test Client")
                    .email("client@example.com")
                    .user(testUser)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClientForCreate));
            when(assetMapper.toEntity(testAssetRequest, null)).thenReturn(testAsset1);
            when(assetRepository.save(any(Asset.class))).thenReturn(testAsset1);
            when(assetMapper.toResponse(any(Asset.class))).thenReturn(testAssetResponse);

            AssetResponse result = assetService.createAsset(1L, testAssetRequest, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("AAPL"); // This comes from the saved asset, not request
            verify(clientRepository).findById(1L);
            verify(assetMapper).toEntity(testAssetRequest, null);
            verify(assetRepository).save(any(Asset.class));
            verify(assetMapper).toResponse(any(Asset.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientDoesNotExist() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.createAsset(1L, testAssetRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client")
                    .hasMessageContaining("1");

            verify(clientRepository).findById(1L);
            verifyNoInteractions(assetRepository, assetMapper);
        }
    }

    @Nested
    @DisplayName("Delete Asset Tests")
    class DeleteAssetTests {

        @Test
        @DisplayName("Should delete asset when asset exists and belongs to user")
        void shouldDeleteAssetWhenAssetExistsAndBelongsToUser() {
            when(assetRepository.existsByIdAndClientUserId(1L, 1L)).thenReturn(true);
            doNothing().when(assetRepository).deleteById(1L);

            assetService.deleteAsset(1L, 1L);

            verify(assetRepository).existsByIdAndClientUserId(1L, 1L);
            verify(assetRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when asset does not exist")
        void shouldThrowExceptionWhenAssetDoesNotExist() {
            when(assetRepository.existsByIdAndClientUserId(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> assetService.deleteAsset(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Asset")
                    .hasMessageContaining("1");

            verify(assetRepository).existsByIdAndClientUserId(1L, 1L);
            verify(assetRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Import Assets Tests")
    class ImportAssetsTests {

        @Test
        @DisplayName("Should import assets successfully")
        void shouldImportAssetsSuccessfully() {
            List<Map<String, Object>> assetsData = Arrays.asList(
                    Map.of("symbol", "AAPL", "quantity", 100, "buyingRate", 150.0),
                    Map.of("symbol", "GOOGL", "quantity", 50, "buyingRate", 2500.0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(assetRepository.save(any(Asset.class))).thenReturn(testAsset1);

            int result = assetService.importAssets(assetsData, 1L);

            assertThat(result).isEqualTo(2);
            verify(userRepository).findById(1L);
            verify(assetRepository, times(2)).save(any(Asset.class));
        }

        @Test
        @DisplayName("Should return 0 when user does not exist")
        void shouldReturn0WhenUserDoesNotExist() {
            List<Map<String, Object>> assetsData = Arrays.asList(
                    Map.of("symbol", "AAPL", "quantity", 100, "buyingRate", 150.0)
            );

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assetService.importAssets(assetsData, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("1");

            verify(userRepository).findById(1L);
            verifyNoInteractions(assetRepository);
        }
    }

    @Nested
    @DisplayName("CSV Import/Export Tests")
    class CsvImportExportTests {

        @Test
        @DisplayName("Should import assets from CSV successfully")
        void shouldImportAssetsFromCSVSuccessfully() throws IOException {
            String csvContent = "symbol,quantity,buyingRate\nAAPL,100,150.00\nGOOGL,50,2500.00";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            when(clientRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(assetRepository.save(any(Asset.class))).thenReturn(testAsset1);

            int result = assetService.importAssetsFromCSV(inputStream, 1L, 1L);

            assertThat(result).isEqualTo(2);
            verify(clientRepository).existsByIdAndUserId(1L, 1L);
            verify(clientRepository).findById(1L);
            verify(assetRepository, times(2)).save(any(Asset.class));
        }

        @Test
        @DisplayName("Should export assets to CSV successfully")
        void shouldExportAssetsToCSVSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList(testAsset1, testAsset2));
            when(pricingService.getCurrentPrice(1L)).thenReturn(new BigDecimal("155.00"));
            when(pricingService.getCurrentPrice(2L)).thenReturn(new BigDecimal("2600.00"));
            when(assetMapper.toResponse(any(Asset.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(testAssetResponse);

            String result = assetService.exportAssetsAsCSV(1L);

            assertThat(result).isNotNull();
            assertThat(result).contains("symbol,quantity,buyingRate");
            verify(userRepository).findById(1L);
            verify(assetRepository).findByClientIdOrderByPurchaseDateDesc(1L);
            verify(pricingService, times(2)).getCurrentPrice(anyLong());
            verify(assetMapper, times(2)).toResponse(any(Asset.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class));
        }
    }
}
