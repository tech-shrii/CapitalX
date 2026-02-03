package com.app.portfolio.service.pricing;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.dto.pricing.*;
import com.app.portfolio.repository.AssetPriceRepository;
import com.app.portfolio.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final AssetRepository assetRepository;
    private final AssetPriceRepository assetPriceRepository;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${pricing.service.url:http://localhost:8000}")
    private String pricingServiceUrl;

    @Value("${pricing.service.enabled:true}")
    private boolean pricingServiceEnabled;

    // Cache for external prices with timestamp
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final long PRICE_CACHE_TTL = 60000; // 60 seconds

    // Service availability tracking
    private final AtomicBoolean serviceAvailable = new AtomicBoolean(true);
    private final AtomicLong lastHealthCheckTime = new AtomicLong(0);
    private static final long HEALTH_CHECK_INTERVAL = 300000; // 5 minutes
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Long assetId) {
        return assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId)
                .map(AssetPrice::getCurrentPrice)
                .orElseGet(() -> {
                    // If no price found, return buying rate as fallback
                    return assetRepository.findById(assetId)
                            .map(Asset::getBuyingRate)
                            .orElse(BigDecimal.ZERO);
                });
    }

    @Override
    @Scheduled(initialDelay = 30000, fixedRate = 20000) // Initial delay 30s, then every 20s
    @Transactional
    public void fetchAndUpdatePrices() {
        try {
            // Check if pricing service is enabled
            if (!pricingServiceEnabled) {
                log.debug("Pricing service is disabled, skipping scheduled update");
                return;
            }

            // Check service availability before attempting update
            if (!isServiceAvailable()) {
                log.debug("Pricing service is unavailable, skipping scheduled update. Using cached/fake prices.");
                return;
            }

            log.debug("Starting scheduled price update...");
            List<Asset> assets = assetRepository.findAll();

            if (assets.isEmpty()) {
                log.debug("No assets found to update prices for");
                return;
            }

            // Collect all unique symbols
            List<String> symbols = assets.stream()
                    .map(Asset::getSymbol)
                    .filter(s -> s != null && !s.isEmpty())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            if (symbols.isEmpty()) {
                return;
            }

            // Use bulk fetch for better performance
            Map<String, Object> bulkResults = bulkFetchPrices(symbols);
            Map<String, Object> pricesData = (Map<String, Object>) bulkResults.get("data");
            
            if (pricesData == null) {
                pricesData = new HashMap<>();
            }

            // Track if we got any successful prices
            boolean hasSuccessfulPrices = false;

            // Update prices for each asset
            for (Asset asset : assets) {
                try {
                    String symbol = asset.getSymbol();
                    if (symbol == null || symbol.isEmpty()) {
                        continue;
                    }

                    Object priceData = pricesData.get(symbol.toUpperCase());
                    BigDecimal price = null;

                    if (priceData instanceof Map) {
                        Map<String, Object> priceMap = (Map<String, Object>) priceData;
                        if (priceMap.containsKey("price") && !priceMap.containsKey("error")) {
                            Object priceObj = priceMap.get("price");
                            if (priceObj instanceof Number) {
                                price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                                hasSuccessfulPrices = true;
                            }
                        }
                    }

                    // Fallback to individual fetch if bulk failed
                    if (price == null) {
                        PriceResponse externalPrice = getCurrentPriceBySymbol(symbol);
                        if (externalPrice != null && externalPrice.getPrice() != null) {
                            price = BigDecimal.valueOf(externalPrice.getPrice());
                            hasSuccessfulPrices = true;
                        }
                    }

                    if (price != null) {
                        AssetPrice assetPrice = AssetPrice.builder()
                                .asset(asset)
                                .currentPrice(price)
                                .priceDate(Instant.now())
                                .source(AssetPrice.PriceSource.YFINANCE)
                                .build();
                        assetPriceRepository.save(assetPrice);
                        log.debug("Updated price for {}: {}", symbol, price);
                    }
                } catch (Exception e) {
                    log.debug("Failed to update price for asset {}: {}", asset.getSymbol(), e.getMessage());
                }
            }

            // Reset failure counter if we got successful prices
            if (hasSuccessfulPrices) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }

            clearExpiredCache();
            log.debug("Price update completed");
        } catch (Exception e) {
            log.error("Error in scheduled price update: {}", e.getMessage(), e);
            recordServiceFailure();
        }
    }

    @Override
    public PriceResponse getCurrentPriceBySymbol(String symbol) {
        try {
            String cacheKey = "PRICE_" + symbol.toUpperCase();
            CachedPrice cached = priceCache.get(cacheKey);

            if (cached != null && !cached.isExpired()) {
                log.debug("Returning cached price for {}", symbol);
                return cached.value;
            }

            // Check service availability
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning null for {}", symbol);
                return null;
            }

            String url = pricingServiceUrl + "/api/price/" + symbol;
            PriceResponse response = restTemplate.getForObject(url, PriceResponse.class);

            if (response != null) {
                priceCache.put(cacheKey, new CachedPrice(response, System.currentTimeMillis()));
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }

            return response;
        } catch (ResourceAccessException e) {
            // Connection refused, service unavailable
            handleConnectionError("Error fetching price for " + symbol, e);
            return null;
        } catch (RestClientException e) {
            // Other REST client errors
            log.warn("REST client error fetching price for {}: {}", symbol, e.getMessage());
            recordServiceFailure();
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching price for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    @Override
    public ChartResponse getChartData(String symbol, String period, String interval) {
        try {
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning null for chart data {}", symbol);
                return null;
            }

            String url = String.format("%s/api/chart/%s?period=%s&interval=%s",
                    pricingServiceUrl, symbol, period, interval);
            ChartResponse response = restTemplate.getForObject(url, ChartResponse.class);
            
            if (response != null) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }
            
            return response;
        } catch (ResourceAccessException e) {
            handleConnectionError("Error fetching chart data for " + symbol, e);
            return null;
        } catch (RestClientException e) {
            log.warn("REST client error fetching chart data for {}: {}", symbol, e.getMessage());
            recordServiceFailure();
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching chart data for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    @Override
    public PortfolioValueResponse getPortfolioValue(Map<String, Double> portfolio) {
        try {
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning null for portfolio value");
                return null;
            }

            String url = pricingServiceUrl + "/api/portfolio/value";
            PortfolioValueResponse response = restTemplate.postForObject(url, portfolio, PortfolioValueResponse.class);
            
            if (response != null) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }
            
            return response;
        } catch (ResourceAccessException e) {
            handleConnectionError("Error fetching portfolio value", e);
            return null;
        } catch (RestClientException e) {
            log.warn("REST client error fetching portfolio value: {}", e.getMessage());
            recordServiceFailure();
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching portfolio value: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PortfolioChartResponse getPortfolioChart(Map<String, Double> portfolio,
                                                     String period, String interval) {
        try {
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning null for portfolio chart");
                return null;
            }

            String url = String.format("%s/api/portfolio/chart?period=%s&interval=%s",
                    pricingServiceUrl, period, interval);
            PortfolioChartResponse response = restTemplate.postForObject(url, portfolio, PortfolioChartResponse.class);
            
            if (response != null) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }
            
            return response;
        } catch (ResourceAccessException e) {
            handleConnectionError("Error fetching portfolio chart", e);
            return null;
        } catch (RestClientException e) {
            log.warn("REST client error fetching portfolio chart: {}", e.getMessage());
            recordServiceFailure();
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching portfolio chart: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getMultiplePrices(List<String> symbols) {
        try {
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning empty map for multiple prices");
                return new HashMap<>();
            }

            String url = pricingServiceUrl + "/api/prices";
            Map<String, Object> response = restTemplate.postForObject(url, symbols, Map.class);
            
            if (response != null && !response.isEmpty()) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }
            
            return response != null ? response : new HashMap<>();
        } catch (ResourceAccessException e) {
            handleConnectionError("Error fetching multiple prices", e);
            return new HashMap<>();
        } catch (RestClientException e) {
            log.warn("REST client error fetching multiple prices: {}", e.getMessage());
            recordServiceFailure();
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Unexpected error fetching multiple prices: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> bulkFetchPrices(List<String> symbols) {
        try {
            if (!isServiceAvailable()) {
                log.debug("Pricing service unavailable, returning empty map for bulk prices");
                return new HashMap<>();
            }

            String url = pricingServiceUrl + "/api/prices/bulk";
            log.debug("Bulk fetching prices for {} symbols", symbols.size());
            Map<String, Object> response = restTemplate.postForObject(url, symbols, Map.class);
            
            if (response != null && !response.isEmpty()) {
                consecutiveFailures.set(0);
                serviceAvailable.set(true);
            }
            
            return response != null ? response : new HashMap<>();
        } catch (ResourceAccessException e) {
            handleConnectionError("Error bulk fetching prices", e);
            return new HashMap<>();
        } catch (RestClientException e) {
            log.warn("REST client error bulk fetching prices: {}", e.getMessage());
            recordServiceFailure();
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Unexpected error bulk fetching prices: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public void clearExpiredCache() {
        priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Check if pricing service is available
     * Performs health check if enough time has passed since last check
     */
    private boolean isServiceAvailable() {
        long currentTime = System.currentTimeMillis();
        long lastCheck = lastHealthCheckTime.get();
        
        // If service is marked as unavailable and enough time hasn't passed, skip check
        if (!serviceAvailable.get() && (currentTime - lastCheck) < HEALTH_CHECK_INTERVAL) {
            return false;
        }
        
        // Perform health check if enough time has passed
        if ((currentTime - lastCheck) >= HEALTH_CHECK_INTERVAL) {
            boolean available = checkServiceHealth();
            lastHealthCheckTime.set(currentTime);
            serviceAvailable.set(available);
            return available;
        }
        
        return serviceAvailable.get();
    }

    /**
     * Perform a health check on the pricing service
     */
    private boolean checkServiceHealth() {
        try {
            // Try the health endpoint first
            String healthUrl = pricingServiceUrl + "/api/health";
            restTemplate.getForObject(healthUrl, Map.class);
            log.debug("Pricing service health check passed");
            consecutiveFailures.set(0);
            return true;
        } catch (ResourceAccessException e) {
            log.debug("Pricing service health check failed (connection error): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // If /api/health endpoint doesn't exist or returns error, try a simple price endpoint
            try {
                String testUrl = pricingServiceUrl + "/api/price/AAPL";
                restTemplate.getForObject(testUrl, Map.class);
                log.debug("Pricing service health check passed (via test endpoint)");
                consecutiveFailures.set(0);
                return true;
            } catch (ResourceAccessException ex) {
                log.debug("Pricing service health check failed (connection error): {}", ex.getMessage());
                return false;
            } catch (Exception ex) {
                log.debug("Pricing service health check failed: {}", ex.getMessage());
                return false;
            }
        }
    }

    /**
     * Handle connection errors (service unavailable)
     */
    private void handleConnectionError(String context, ResourceAccessException e) {
        String errorMsg = e.getMessage();
        boolean isConnectionRefused = errorMsg != null && 
            (errorMsg.contains("Connection refused") || 
             errorMsg.contains("connect") ||
             errorMsg.contains("I/O error"));
        
        if (isConnectionRefused) {
            // Use WARN level for connection refused (service unavailable) - less noisy than ERROR
            log.warn("{} - Pricing service unavailable (connection refused). Service may be down. {}", 
                    context, pricingServiceUrl);
            recordServiceFailure();
        } else {
            log.error("{} - Connection error: {}", context, e.getMessage());
            recordServiceFailure();
        }
    }

    /**
     * Record a service failure and update availability status
     */
    private void recordServiceFailure() {
        long failures = consecutiveFailures.incrementAndGet();
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            serviceAvailable.set(false);
            log.warn("Pricing service marked as unavailable after {} consecutive failures. " +
                    "Will retry health check in {} minutes.", failures, HEALTH_CHECK_INTERVAL / 60000);
        }
    }

    /**
     * Inner class for caching prices with TTL
     */
    private static class CachedPrice {
        private final PriceResponse value;
        private final long timestamp;

        CachedPrice(PriceResponse value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > PRICE_CACHE_TTL; // 60 seconds
        }
    }
}
