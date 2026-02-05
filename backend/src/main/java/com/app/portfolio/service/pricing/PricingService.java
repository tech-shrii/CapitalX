package com.app.portfolio.service.pricing;

import com.app.portfolio.dto.pricing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PricingService {

    BigDecimal getCurrentPrice(Long assetId);

    /**
     * Get current price by symbol as BigDecimal (internal use)
     * This allows multiple users to share the same price data for the same symbol
     */
    BigDecimal getCurrentPriceBySymbolAsBigDecimal(String symbol);

    void fetchAndUpdatePrices();
    
    // New methods for external pricing service
    PriceResponse getCurrentPriceBySymbol(String symbol);
    
    ChartResponse getChartData(String symbol, String period, String interval);
    
    PortfolioValueResponse getPortfolioValue(Map<String, Double> portfolio);
    
    PortfolioChartResponse getPortfolioChart(Map<String, Double> portfolio, String period, String interval);
    
    /**
     * Get historical portfolio chart from database (for 6 months display)
     * Aggregates all prices from database for the given symbols and quantities
     * Only fetches live price for today's date
     */
    PortfolioChartResponse getPortfolioChartFromDatabase(Map<String, Double> portfolio, String period, String interval);
    
    /**
     * Get client portfolio chart from database using only MANUAL source data
     * For 6-month period, uses 1-week intervals
     * Aggregates price * quantity for all assets and sums them up
     * Only fetches live price for today's date
     */
    PortfolioChartResponse getClientPortfolioChartFromDatabase(Map<String, Double> portfolio, String period);
    
    Map<String, Object> getMultiplePrices(List<String> symbols);
    
    /**
     * Bulk fetch prices for all symbols at once (optimized for login/dashboard)
     * Uses batch download for better performance
     */
    Map<String, Object> bulkFetchPrices(List<String> symbols);
    
    void clearExpiredCache();
}
