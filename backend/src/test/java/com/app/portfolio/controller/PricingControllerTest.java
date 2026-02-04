package com.app.portfolio.controller;

import com.app.portfolio.dto.pricing.*;
import com.app.portfolio.service.pricing.PricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PricingControllerTest {

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private PricingController pricingController;

    @org.junit.jupiter.api.Test
    @DisplayName("Get price returns price response for valid symbol")
    void getPrice_ReturnsPriceResponse_ForValidSymbol() {
        String symbol = "AAPL";
        PriceResponse priceResponse = mock(PriceResponse.class);
        when(pricingService.getCurrentPriceBySymbol(symbol)).thenReturn(priceResponse);

        ResponseEntity<PriceResponse> response = pricingController.getPrice(symbol);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(priceResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get price throws exception when service throws exception")
    void getPrice_ThrowsException_WhenServiceThrowsException() {
        String symbol = "AAPL";
        when(pricingService.getCurrentPriceBySymbol(symbol)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.getPrice(symbol));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get chart returns chart response for valid symbol and params")
    void getChart_ReturnsChartResponse_ForValidSymbolAndParams() {
        String symbol = "AAPL";
        String period = "1mo";
        String interval = "1d";
        ChartResponse chartResponse = mock(ChartResponse.class);
        when(pricingService.getChartData(symbol, period, interval)).thenReturn(chartResponse);

        ResponseEntity<ChartResponse> response = pricingController.getChart(symbol, period, interval);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(chartResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get chart throws exception when service throws exception")
    void getChart_ThrowsException_WhenServiceThrowsException() {
        String symbol = "AAPL";
        String period = "1mo";
        String interval = "1d";
        when(pricingService.getChartData(symbol, period, interval)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.getChart(symbol, period, interval));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get prices returns map for valid symbols")
    void getPrices_ReturnsMap_ForValidSymbols() {
        List<String> symbols = List.of("AAPL", "MSFT");
        Map<String, Object> prices = Map.of("AAPL", 100, "MSFT", 200);
        when(pricingService.getMultiplePrices(symbols)).thenReturn(prices);

        ResponseEntity<Map<String, Object>> response = pricingController.getPrices(symbols);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(prices);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get prices throws exception when service throws exception")
    void getPrices_ThrowsException_WhenServiceThrowsException() {
        List<String> symbols = List.of("AAPL", "MSFT");
        when(pricingService.getMultiplePrices(symbols)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.getPrices(symbols));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Bulk fetch prices returns map for valid symbols")
    void bulkFetchPrices_ReturnsMap_ForValidSymbols() {
        List<String> symbols = List.of("AAPL", "MSFT");
        Map<String, Object> prices = Map.of("AAPL", 100, "MSFT", 200);
        when(pricingService.bulkFetchPrices(symbols)).thenReturn(prices);

        ResponseEntity<Map<String, Object>> response = pricingController.bulkFetchPrices(symbols);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(prices);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Bulk fetch prices throws exception when service throws exception")
    void bulkFetchPrices_ThrowsException_WhenServiceThrowsException() {
        List<String> symbols = List.of("AAPL", "MSFT");
        when(pricingService.bulkFetchPrices(symbols)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.bulkFetchPrices(symbols));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get portfolio value returns response for valid portfolio")
    void getPortfolioValue_ReturnsResponse_ForValidPortfolio() {
        Map<String, Double> portfolio = Map.of("AAPL", 10.0, "MSFT", 5.0);
        PortfolioValueResponse valueResponse = mock(PortfolioValueResponse.class);
        when(pricingService.getPortfolioValue(portfolio)).thenReturn(valueResponse);

        ResponseEntity<PortfolioValueResponse> response = pricingController.getPortfolioValue(portfolio);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(valueResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get portfolio value throws exception when service throws exception")
    void getPortfolioValue_ThrowsException_WhenServiceThrowsException() {
        Map<String, Double> portfolio = Map.of("AAPL", 10.0, "MSFT", 5.0);
        when(pricingService.getPortfolioValue(portfolio)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.getPortfolioValue(portfolio));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get portfolio chart returns response for valid portfolio and params")
    void getPortfolioChart_ReturnsResponse_ForValidPortfolioAndParams() {
        Map<String, Double> portfolio = Map.of("AAPL", 10.0, "MSFT", 5.0);
        String period = "1mo";
        String interval = "1d";
        PortfolioChartResponse chartResponse = mock(PortfolioChartResponse.class);
        when(pricingService.getPortfolioChart(portfolio, period, interval)).thenReturn(chartResponse);

        ResponseEntity<PortfolioChartResponse> response = pricingController.getPortfolioChart(portfolio, period, interval);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(chartResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get portfolio chart throws exception when service throws exception")
    void getPortfolioChart_ThrowsException_WhenServiceThrowsException() {
        Map<String, Double> portfolio = Map.of("AAPL", 10.0, "MSFT", 5.0);
        String period = "1mo";
        String interval = "1d";
        when(pricingService.getPortfolioChart(portfolio, period, interval)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> pricingController.getPortfolioChart(portfolio, period, interval));
    }
}
