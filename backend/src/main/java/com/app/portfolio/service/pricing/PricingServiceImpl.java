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
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    // Cache for external prices with timestamp
    private final Map<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final long PRICE_CACHE_TTL = 60000; // 60 seconds

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Long assetId) {
        return assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId)
                .map(assetPrice -> {
                    // Add random variation for real-time simulation (±0.5% to ±2%)
                    BigDecimal basePrice = assetPrice.getCurrentPrice();
                    double variationPercent = (random.nextDouble() * 0.03 - 0.015); // -1.5% to +1.5%
                    BigDecimal variation = basePrice.multiply(BigDecimal.valueOf(variationPercent));
                    BigDecimal currentPrice = basePrice.add(variation);
                    return currentPrice.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
                })
                .orElseGet(() -> {
                    // If no price found, return buying rate as fallback
                    return assetRepository.findById(assetId)
                            .map(Asset::getBuyingRate)
                            .orElse(BigDecimal.ZERO);
                });
    }

    @Override
    @Scheduled(initialDelay = 30000, fixedRate = 60000) // Initial delay 30s, then every 1m
    @Transactional
    public void fetchAndUpdatePrices() {
        try {
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
                            }
                        }
                    }

                    // Fallback to individual fetch if bulk failed
                    if (price == null) {
                        PriceResponse externalPrice = getCurrentPriceBySymbol(symbol);
                        if (externalPrice != null && externalPrice.getPrice() != null) {
                            price = BigDecimal.valueOf(externalPrice.getPrice());
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
                    } else {
                        // Use fake data as fallback
                        BigDecimal fakePrice = generateFakePrice(asset);
                        AssetPrice assetPrice = AssetPrice.builder()
                                .asset(asset)
                                .currentPrice(fakePrice)
                                .priceDate(Instant.now())
                                .source(AssetPrice.PriceSource.FAKE)
                                .build();
                        assetPriceRepository.save(assetPrice);
                    }
                } catch (Exception e) {
                    log.debug("Failed to update price for asset {}: {}", asset.getSymbol(), e.getMessage());
                }
            }
            clearExpiredCache();
            log.debug("Price update completed");
        } catch (Exception e) {
            log.error("Error in scheduled price update: {}", e.getMessage(), e);
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

            String url = pricingServiceUrl + "/api/price/" + symbol;
            PriceResponse response = restTemplate.getForObject(url, PriceResponse.class);

            if (response != null) {
                priceCache.put(cacheKey, new CachedPrice(response, System.currentTimeMillis()));
            }

            return response;
        } catch (Exception e) {
            log.error("Error fetching price for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    @Override
    public ChartResponse getChartData(String symbol, String period, String interval) {
        try {
            String url = String.format("%s/api/chart/%s?period=%s&interval=%s",
                    pricingServiceUrl, symbol, period, interval);
            return restTemplate.getForObject(url, ChartResponse.class);
        } catch (Exception e) {
            log.error("Error fetching chart data for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    @Override
    public PortfolioValueResponse getPortfolioValue(Map<String, Double> portfolio) {
        try {
            String url = pricingServiceUrl + "/api/portfolio/value";
            return restTemplate.postForObject(url, portfolio, PortfolioValueResponse.class);
        } catch (Exception e) {
            log.error("Error fetching portfolio value: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public PortfolioChartResponse getPortfolioChart(Map<String, Double> portfolio,
                                                     String period, String interval) {
        try {
            String url = String.format("%s/api/portfolio/chart?period=%s&interval=%s",
                    pricingServiceUrl, period, interval);
            return restTemplate.postForObject(url, portfolio, PortfolioChartResponse.class);
        } catch (Exception e) {
            log.error("Error fetching portfolio chart: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getMultiplePrices(List<String> symbols) {
        try {
            String url = pricingServiceUrl + "/api/prices";
            return restTemplate.postForObject(url, symbols, Map.class);
        } catch (Exception e) {
            log.error("Error fetching multiple prices: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> bulkFetchPrices(List<String> symbols) {
        try {
            String url = pricingServiceUrl + "/api/prices/bulk";
            log.debug("Bulk fetching prices for {} symbols", symbols.size());
            Map<String, Object> response = restTemplate.postForObject(url, symbols, Map.class);
            if (response == null) {
                return new HashMap<>();
            }
            return response;
        } catch (Exception e) {
            log.error("Error bulk fetching prices: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public void clearExpiredCache() {
        priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private BigDecimal generateFakePrice(Asset asset) {
        BigDecimal basePrice = asset.getBuyingRate();
        double variation = (random.nextDouble() * 0.2 - 0.1); // -10% to +10%
        BigDecimal newPrice = basePrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(variation)));
        return newPrice.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
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
