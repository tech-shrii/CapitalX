package com.app.portfolio.controller;

import com.app.portfolio.dto.pricing.*;
import com.app.portfolio.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PricingController {

    private final PricingService pricingService;

    /**
     * Get current price for a symbol
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<PriceResponse> getPrice(@PathVariable String symbol) {
        log.debug("Fetching current price for: {}", symbol);
        PriceResponse response = pricingService.getCurrentPriceBySymbol(symbol);
        return ResponseEntity.ok(response);
    }

    /**
     * Get chart data for a symbol
     */
    @GetMapping("/chart/{symbol}")
    public ResponseEntity<ChartResponse> getChart(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1mo") String period,
            @RequestParam(defaultValue = "1d") String interval) {
        log.debug("Fetching chart for: {} with period: {} and interval: {}", symbol, period, interval);
        ChartResponse response = pricingService.getChartData(symbol, period, interval);
        return ResponseEntity.ok(response);
    }

    /**
     * Get prices for multiple symbols
     */
    @PostMapping("/prices")
    public ResponseEntity<Map<String, Object>> getPrices(@RequestBody List<String> symbols) {
        log.debug("Fetching prices for {} symbols", symbols.size());
        Map<String, Object> response = pricingService.getMultiplePrices(symbols);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk fetch prices for all symbols at once (optimized for login/dashboard)
     * Uses batch download for better performance
     */
    @PostMapping("/prices/bulk")
    public ResponseEntity<Map<String, Object>> bulkFetchPrices(@RequestBody List<String> symbols) {
        log.debug("Bulk fetching prices for {} symbols", symbols.size());
        Map<String, Object> response = pricingService.bulkFetchPrices(symbols);
        return ResponseEntity.ok(response);
    }

    /**
     * Get portfolio total value and breakdown
     * Expects: {"AAPL": 10, "MSFT": 5} (symbol: quantity)
     */
    @PostMapping("/portfolio/value")
    public ResponseEntity<PortfolioValueResponse> getPortfolioValue(
            @RequestBody Map<String, Double> portfolio) {
        log.debug("Calculating portfolio value for {} assets", portfolio.size());
        PortfolioValueResponse response = pricingService.getPortfolioValue(portfolio);
        return ResponseEntity.ok(response);
    }

    /**
     * Get combined historical chart for portfolio from database (MANUAL source)
     * Expects: {"AAPL": 10, "MSFT": 5}
     * Now uses database data instead of external pricing service
     */
    @PostMapping("/portfolio/chart")
    public ResponseEntity<PortfolioChartResponse> getPortfolioChart(
            @RequestBody Map<String, Double> portfolio,
            @RequestParam(defaultValue = "6mo") String period,
            @RequestParam(defaultValue = "1wk") String interval) {
        log.debug("Fetching portfolio chart from database with period: {} and interval: {}", period, interval);
        PortfolioChartResponse response = pricingService.getPortfolioChartFromDatabase(portfolio, period, interval);
        return ResponseEntity.ok(response);
    }
}
