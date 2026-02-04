package com.app.portfolio.service;

import com.app.portfolio.beans.*;
import com.app.portfolio.dto.asset.*;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.AssetMapper;
import com.app.portfolio.repository.*;
import com.app.portfolio.service.asset.AssetServiceImpl;
import com.app.portfolio.service.pricing.PricingService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetServiceImplTest {

    @Mock private AssetRepository assetRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssetMapper assetMapper;
    @Mock private PricingService pricingService;

    @InjectMocks private AssetServiceImpl assetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createAsset_shouldReturnAssetResponse_whenValidRequest() {
        AssetRequest request = new AssetRequest();
        Client client = new Client();
        User user = new User();
        user.setId(2L);
        client.setUser(user);
        Asset asset = new Asset();
        Asset savedAsset = new Asset();
        AssetResponse response = mock(AssetResponse.class);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetMapper.toEntity(request, null)).thenReturn(asset);
        when(assetRepository.save(asset)).thenReturn(savedAsset);
        when(assetMapper.toResponse(savedAsset)).thenReturn(response);

        AssetResponse result = assetService.createAsset(1L, request, 2L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void createAsset_shouldThrow_whenClientNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.createAsset(1L, new AssetRequest(), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAsset_shouldThrow_whenClientNotBelongToUser() {
        AssetRequest request = new AssetRequest();
        Client client = new Client();
        User user = new User();
        user.setId(99L);
        client.setUser(user);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        assertThatThrownBy(() -> assetService.createAsset(1L, request, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAsset_shouldReturnUpdatedAssetResponse_whenValid() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setBuyingRate(BigDecimal.TEN);
        asset.setQuantity(BigDecimal.ONE);
        AssetRequest request = new AssetRequest();
        Asset updatedAsset = new Asset();
        AssetResponse response = mock(AssetResponse.class);

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(true);
        when(assetMapper.toEntity(request, asset)).thenReturn(updatedAsset);
        when(assetRepository.save(updatedAsset)).thenReturn(updatedAsset);
        when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.valueOf(12));
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        AssetResponse result = assetService.updateAsset(1L, request, 2L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateAsset_shouldThrow_whenAssetNotFound() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.updateAsset(1L, new AssetRequest(), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAsset_shouldThrow_whenAssetNotBelongToUser() {
        Asset asset = new Asset();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> assetService.updateAsset(1L, new AssetRequest(), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAsset_shouldDelete_whenAssetBelongsToUser() {
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(true);
        assetService.deleteAsset(1L, 2L);
        verify(assetRepository).deleteById(1L);
    }

    @Test
    void deleteAsset_shouldThrow_whenAssetNotBelongToUser() {
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> assetService.deleteAsset(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAssetById_shouldReturnAssetResponse_whenAssetBelongsToUser() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setBuyingRate(BigDecimal.TEN);
        asset.setQuantity(BigDecimal.ONE);
        AssetResponse response = mock(AssetResponse.class);

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(true);
        when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.valueOf(12));
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        AssetResponse result = assetService.getAssetById(1L, 2L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getAssetById_shouldThrow_whenAssetNotFound() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.getAssetById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAssetById_shouldThrow_whenAssetNotBelongToUser() {
        Asset asset = new Asset();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByIdAndClientUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> assetService.getAssetById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAssetsByClientId_shouldReturnList_whenClientBelongsToUser() {
        when(clientRepository.existsByIdAndUserId(1L, 2L)).thenReturn(true);
        Asset asset = new Asset();
        List<Asset> assets = List.of(asset);
        AssetResponse response = mock(AssetResponse.class);
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(assets);
        when(pricingService.getCurrentPrice(anyLong())).thenReturn(BigDecimal.TEN);
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        List<AssetResponse> result = assetService.getAssetsByClientId(1L, 2L);

        assertThat(result).containsExactly(response);
    }

    @Test
    void getAssetsByClientId_shouldThrow_whenClientNotBelongToUser() {
        when(clientRepository.existsByIdAndUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> assetService.getAssetsByClientId(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllAssets_shouldReturnAssetsForUser() {
        User user = new User();
        user.setId(2L);
        Client client = new Client();
        client.setId(1L);
        user.setClients(List.of(client));
        Asset asset = new Asset();
        AssetResponse response = mock(AssetResponse.class);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(List.of(asset));
        when(pricingService.getCurrentPrice(anyLong())).thenReturn(BigDecimal.TEN);
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(response);

        List<AssetResponse> result = assetService.getAllAssets(2L);

        assertThat(result).containsExactly(response);
    }

    @Test
    void getAllAssets_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.getAllAssets(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculatePnL_shouldReturnCorrectResponse_whenValid() {
        Client client = new Client();
        client.setId(1L);
        User user = new User();
        user.setId(2L);
        client.setUser(user);

        Asset asset = new Asset();
        asset.setId(3L);
        asset.setBuyingRate(BigDecimal.TEN);
        asset.setQuantity(BigDecimal.ONE);
        asset.setClient(client);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(List.of(asset));
        when(pricingService.getCurrentPrice(3L)).thenReturn(BigDecimal.valueOf(12));
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(mock(AssetResponse.class));

        PnlResponse response = assetService.calculatePnL(1L, 2L);

        assertThat(response.getTotalInvested()).isEqualTo(BigDecimal.TEN);
        assertThat(response.getTotalCurrentValue()).isEqualTo(BigDecimal.valueOf(12));
        assertThat(response.getTotalProfitLoss()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(response.getAssets()).hasSize(1);
    }

    @Test
    void calculatePnL_shouldThrow_whenClientNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.calculatePnL(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculatePnL_shouldThrow_whenClientNotBelongToUser() {
        Client client = new Client();
        User user = new User();
        user.setId(99L);
        client.setUser(user);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        assertThatThrownBy(() -> assetService.calculatePnL(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importAssets_shouldReturnCount_whenAssetsImported() {
        User user = new User();
        user.setId(2L);
        Client client = new Client();
        client.setId(1L);
        client.setUser(user);
        user.setClients(List.of(client));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(assetMapper.toEntity(any(), isNull())).thenReturn(new Asset());
        when(assetRepository.save(any())).thenReturn(new Asset());

        Map<String, Object> assetData = new HashMap<>();
        assetData.put("symbol", "AAPL");
        assetData.put("quantity", 10);
        assetData.put("buyingRate", 100);
        assetData.put("assetType", "STOCK");
        assetData.put("purchaseDate", LocalDate.now().toString());
        assetData.put("currency", "USD");

        int count = assetService.importAssets(List.of(assetData), 2L);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void importAssets_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assetService.importAssets(List.of(new HashMap<>()), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exportAssetsAsCSV_shouldReturnCSVString() {
        AssetResponse assetResponse = mock(AssetResponse.class);
        when(assetResponse.getSymbol()).thenReturn("AAPL");
        when(assetResponse.getQuantity()).thenReturn(BigDecimal.TEN);
        when(assetResponse.getBuyingRate()).thenReturn(BigDecimal.valueOf(100));
        //when(assetResponse.getAssetType()).thenReturn(Asset.AssetCategory.STOCK);
        when(assetResponse.getPurchaseDate()).thenReturn(LocalDate.now());
        when(assetResponse.getCurrency()).thenReturn("USD");
        when(assetMapper.toResponse(any(), any(), any(), any())).thenReturn(assetResponse);

        AssetServiceImpl assetServiceSpy = Mockito.spy(assetService);
        doReturn(List.of(assetResponse)).when(assetServiceSpy).getAllAssets(2L);

        String csv = assetServiceSpy.exportAssetsAsCSV(2L);

        assertThat(csv).contains("symbol,quantity,buyingRate,assetType,purchaseDate,currency");
        assertThat(csv).contains("AAPL");
    }
}
