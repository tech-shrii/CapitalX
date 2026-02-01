package com.app.portfolio.service.pricing;

import com.app.portfolio.dto.pricing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PricingService {

    BigDecimal getCurrentPrice(Long assetId);

    void fetchAndUpdatePrices();
    
    // New methods for external pricing service
    PriceResponse getCurrentPriceBySymbol(String symbol);
    
    ChartResponse getChartData(String symbol, String period, String interval);
    
    PortfolioValueResponse getPortfolioValue(Map<String, Double> portfolio);
    
    PortfolioChartResponse getPortfolioChart(Map<String, Double> portfolio, String period, String interval);
    
    Map<String, Object> getMultiplePrices(List<String> symbols);
    
    /**
     * Bulk fetch prices for all symbols at once (optimized for login/dashboard)
     * Uses batch download for better performance
     */
    Map<String, Object> bulkFetchPrices(List<String> symbols);
    
    void clearExpiredCache();
}
