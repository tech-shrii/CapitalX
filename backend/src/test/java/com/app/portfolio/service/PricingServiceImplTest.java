package com.app.portfolio.service;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.dto.pricing.PriceResponse;
import com.app.portfolio.repository.AssetPriceRepository;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.service.pricing.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PricingServiceImplTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetPriceRepository assetPriceRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PricingServiceImpl pricingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Get current price for asset with existing price")
    void getCurrentPriceForAssetWithExistingPrice() {
        Long assetId = 1L;
        AssetPrice assetPrice = new AssetPrice();
        assetPrice.setCurrentPrice(BigDecimal.valueOf(150.75));

        when(assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId))
                .thenReturn(java.util.Optional.of(assetPrice));

        BigDecimal price = pricingService.getCurrentPrice(assetId);

        assertNotNull(price);
        assertEquals(BigDecimal.valueOf(150.75), price);
    }

    @Test
    @DisplayName("Get current price for asset with no price but existing buying rate")
    void getCurrentPriceForAssetWithNoPriceButExistingBuyingRate() {
        Long assetId = 1L;
        Asset asset = new Asset();
        asset.setBuyingRate(BigDecimal.valueOf(100.50));

        when(assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId))
                .thenReturn(java.util.Optional.empty());
        when(assetRepository.findById(assetId)).thenReturn(java.util.Optional.of(asset));

        BigDecimal price = pricingService.getCurrentPrice(assetId);

        assertNotNull(price);
        assertEquals(BigDecimal.valueOf(100.50), price);
    }

    @Test
    @DisplayName("Get current price for asset with no price and no buying rate")
    void getCurrentPriceForAssetWithNoPriceAndNoBuyingRate() {
        Long assetId = 1L;

        when(assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId))
                .thenReturn(java.util.Optional.empty());
        when(assetRepository.findById(assetId)).thenReturn(java.util.Optional.empty());

        BigDecimal price = pricingService.getCurrentPrice(assetId);

        assertNotNull(price);
        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    @DisplayName("Fetch and update prices when service is disabled")
    void fetchAndUpdatePricesWhenServiceIsDisabled() {
        //pricingService.setPricingServiceEnabled(false);

        pricingService.fetchAndUpdatePrices();

        verifyNoInteractions(assetRepository, restTemplate, assetPriceRepository);
    }

    @Test
    @DisplayName("Get current price by symbol with valid response")
    void getCurrentPriceBySymbolWithValidResponse() {
        String symbol = "AAPL";
        PriceResponse priceResponse = new PriceResponse();
        priceResponse.setPrice(200.25);

        when(restTemplate.getForObject(anyString(), eq(PriceResponse.class)))
                .thenReturn(priceResponse);

        PriceResponse response = pricingService.getCurrentPriceBySymbol(symbol);

        assertNotNull(response);
        assertEquals(200.25, response.getPrice());
    }

    @Test
    @DisplayName("Get current price by symbol when service is unavailable")
    void getCurrentPriceBySymbolWhenServiceIsUnavailable() {
        //pricingService.setServiceAvailable(false);

        PriceResponse response = pricingService.getCurrentPriceBySymbol("AAPL");

        assertNull(response);
    }

    @Test
    @DisplayName("Get portfolio value with valid response")
    void getPortfolioValueWithValidResponse() {
        Map<String, Double> portfolio = Map.of("AAPL", 10.0, "GOOGL", 5.0);
        when(restTemplate.postForObject(anyString(), eq(portfolio), eq(Map.class)))
                .thenReturn(Map.of("totalValue", 5000.0));

        var response = pricingService.getPortfolioValue(portfolio);

        assertNotNull(response);
        assertEquals(5000.0, response.getTotalValue());
    }
}
