package com.app.portfolio.service.pricing;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.dto.pricing.*;
import com.app.portfolio.repository.AssetPriceRepository;
import com.app.portfolio.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Value("${pricing.migration.enabled:true}")
    private boolean migrationEnabled;

    /**
     * Run migration on startup to populate symbols in existing asset_prices records
     * This ensures existing price data is preserved when migrating to symbol-based pricing
     */
    @PostConstruct
    public void runMigrationOnStartup() {
        if (migrationEnabled) {
            try {
                log.info("Checking for asset prices that need symbol migration...");
                List<AssetPrice> pricesWithoutSymbol = assetPriceRepository.findAllWithoutSymbol();
                if (!pricesWithoutSymbol.isEmpty()) {
                    log.info("Found {} asset prices without symbols, running migration...", pricesWithoutSymbol.size());
                    migrateAssetPricesWithSymbols();
                } else {
                    log.info("No migration needed - all asset prices already have symbols");
                }
            } catch (Exception e) {
                log.error("Error during startup migration: {}", e.getMessage(), e);
                // Don't fail startup if migration fails - it can be run manually later
            }
        } else {
            log.info("Asset price migration is disabled");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Long assetId) {
        // Get asset to retrieve symbol
        Asset asset = assetRepository.findById(assetId).orElse(null);
        if (asset == null || asset.getSymbol() == null || asset.getSymbol().isEmpty()) {
            // Fallback to buying rate if asset not found or no symbol
            return asset != null ? asset.getBuyingRate() : BigDecimal.ZERO;
        }
        
        // Use symbol-based pricing (preferred method)
        return getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPriceBySymbolAsBigDecimal(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return assetPriceRepository.findFirstBySymbolOrderByPriceDateDesc(symbol.toUpperCase())
                .map(AssetPrice::getCurrentPrice)
                .orElse(BigDecimal.ZERO);
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
                        // Check if price already exists for this symbol (to avoid duplicates)
                        Optional<AssetPrice> existingPrice = assetPriceRepository
                                .findFirstBySymbolOrderByPriceDateDesc(symbol.toUpperCase());
                        
                        // Only create new price entry if:
                        // 1. No price exists for this symbol, OR
                        // 2. Existing price is older than 1 minute (to avoid too frequent updates)
                        boolean shouldCreateNew = true;
                        if (existingPrice.isPresent()) {
                            AssetPrice existing = existingPrice.get();
                            long timeDiff = Instant.now().toEpochMilli() - existing.getPriceDate().toEpochMilli();
                            if (timeDiff < 60000) { // Less than 1 minute
                                shouldCreateNew = false;
                                log.debug("Price for {} updated recently, skipping duplicate entry", symbol);
                            }
                        }
                        
                        if (shouldCreateNew) {
                            AssetPrice assetPrice = AssetPrice.builder()
                                    .asset(asset) // Keep asset reference for backward compatibility
                                    .symbol(symbol.toUpperCase()) // Store symbol for symbol-based queries
                                    .currentPrice(price)
                                    .priceDate(Instant.now())
                                    .source(AssetPrice.PriceSource.YFINANCE)
                                    .build();
                            assetPriceRepository.save(assetPrice);
                            log.debug("Updated price for {}: {}", symbol, price);
                        }
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
    @Transactional(readOnly = true)
    public PortfolioChartResponse getPortfolioChartFromDatabase(Map<String, Double> portfolio,
                                                                 String period, String interval) {
        try {
            log.debug("Fetching portfolio chart from database for period: {}, interval: {}", period, interval);
            
            if (portfolio == null || portfolio.isEmpty()) {
                log.warn("Empty portfolio provided for chart");
                return new PortfolioChartResponse(portfolio, period, interval, Collections.emptyList());
            }
            
            // Calculate date range based on period
            Instant endDate = Instant.now();
            LocalDate endLocalDate = endDate.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate startLocalDate;
            
            switch (period.toLowerCase()) {
                case "6mo":
                case "6m":
                    startLocalDate = endLocalDate.minusMonths(6);
                    break;
                case "1y":
                    startLocalDate = endLocalDate.minusYears(1);
                    break;
                case "1mo":
                case "1m":
                    startLocalDate = endLocalDate.minusMonths(1);
                    break;
                case "1w":
                    startLocalDate = endLocalDate.minusWeeks(1);
                    break;
                case "1d":
                    startLocalDate = endLocalDate.minusDays(1);
                    break;
                case "5y":
                    startLocalDate = endLocalDate.minusYears(5);
                    break;
                default:
                    startLocalDate = endLocalDate.minusMonths(6); // Default to 6 months
            }
            
            Instant startDate = startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            log.debug("Date range: {} to {}", startDate, endDate);
            
            // Get today's date (without time) for comparison
            LocalDate today = LocalDate.now();
            Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            // Map to store aggregated portfolio values by date
            Map<LocalDate, Double> portfolioValuesByDate = new TreeMap<>();
            
            // Fetch historical prices for each symbol from database
            log.debug("Processing portfolio with {} symbols", portfolio.size());
            for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
                String symbol = entry.getKey().toUpperCase();
                Double quantity = entry.getValue();
                
                if (symbol == null || symbol.isEmpty() || quantity == null || quantity <= 0) {
                    log.debug("Skipping invalid symbol/quantity: symbol={}, quantity={}", symbol, quantity);
                    continue;
                }
                
                // Fetch historical prices from database (MANUAL source only for historical data)
                List<AssetPrice> historicalPrices = assetPriceRepository.findBySymbolAndDateRangeManual(
                        symbol, startDate, endDate);
                
                log.debug("Found {} MANUAL historical prices for symbol {} (date range: {} to {})", 
                        historicalPrices.size(), symbol, startDate, endDate);
                
                // Aggregate prices by date
                for (AssetPrice price : historicalPrices) {
                    LocalDate priceDate = price.getPriceDate()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    
                    // Skip today's date - we'll use live price for today
                    if (priceDate.equals(today)) {
                        continue;
                    }
                    
                    double value = price.getCurrentPrice().doubleValue() * quantity;
                    portfolioValuesByDate.merge(priceDate, value, Double::sum);
                }
            }
            
            // Fetch live prices for today only
            Map<String, Double> todayPrices = new HashMap<>();
            for (String symbol : portfolio.keySet()) {
                try {
                    PriceResponse priceResponse = getCurrentPriceBySymbol(symbol);
                    if (priceResponse != null && priceResponse.getPrice() != null) {
                        BigDecimal livePrice = BigDecimal.valueOf(priceResponse.getPrice());
                        if (livePrice.compareTo(BigDecimal.ZERO) > 0) {
                            todayPrices.put(symbol, livePrice.doubleValue());
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch live price for {}: {}", symbol, e.getMessage());
                }
            }
            
            // Add today's aggregated value
            if (!todayPrices.isEmpty()) {
                double todayValue = todayPrices.entrySet().stream()
                        .mapToDouble(e -> e.getValue() * portfolio.get(e.getKey()))
                        .sum();
                portfolioValuesByDate.put(today, todayValue);
            }
            
            // For 6-month period with 1-week intervals, aggregate data weekly
            if ((period.equalsIgnoreCase("6mo") || period.equalsIgnoreCase("6m")) && interval.equals("1wk")) {
                Map<LocalDate, Double> weeklyAggregated = new TreeMap<>();
                
                // Group by week start (Monday) and keep the last value of each week
                for (Map.Entry<LocalDate, Double> entry : portfolioValuesByDate.entrySet()) {
                    LocalDate weekStart = entry.getKey().with(java.time.DayOfWeek.MONDAY);
                    weeklyAggregated.put(entry.getKey(), entry.getValue());
                }
                
                // Now group by week and keep only the last day of each week
                Map<LocalDate, Double> finalWeekly = new TreeMap<>();
                LocalDate currentWeekStart = null;
                LocalDate lastDateInWeek = null;
                Double lastValueInWeek = null;
                
                for (Map.Entry<LocalDate, Double> entry : weeklyAggregated.entrySet()) {
                    LocalDate weekStart = entry.getKey().with(java.time.DayOfWeek.MONDAY);
                    
                    if (currentWeekStart == null || !currentWeekStart.equals(weekStart)) {
                        // Save previous week's last value
                        if (currentWeekStart != null && lastDateInWeek != null) {
                            finalWeekly.put(lastDateInWeek, lastValueInWeek);
                        }
                        // Start new week
                        currentWeekStart = weekStart;
                        lastDateInWeek = entry.getKey();
                        lastValueInWeek = entry.getValue();
                    } else {
                        // Same week, update to latest date
                        lastDateInWeek = entry.getKey();
                        lastValueInWeek = entry.getValue();
                    }
                }
                
                // Don't forget the last week
                if (lastDateInWeek != null) {
                    finalWeekly.put(lastDateInWeek, lastValueInWeek);
                }
                
                portfolioValuesByDate = finalWeekly;
                log.debug("Weekly aggregation: {} data points after aggregation", finalWeekly.size());
            }
            
            log.debug("Total portfolio values by date: {} entries", portfolioValuesByDate.size());
            
            // Convert to PortfolioChartDataPoint list
            List<com.app.portfolio.dto.pricing.PortfolioChartDataPoint> chartData = 
                    portfolioValuesByDate.entrySet().stream()
                            .map(entry -> {
                                com.app.portfolio.dto.pricing.PortfolioChartDataPoint point = 
                                        new com.app.portfolio.dto.pricing.PortfolioChartDataPoint();
                                point.setTime(entry.getKey().atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toString());
                                point.setValue(entry.getValue());
                                return point;
                            })
                            .collect(Collectors.toList());
            
            log.debug("Generated {} data points for portfolio chart", chartData.size());
            
            return new PortfolioChartResponse(portfolio, period, interval, chartData);
                    
        } catch (Exception e) {
            log.error("Error fetching portfolio chart from database: {}", e.getMessage(), e);
            return new PortfolioChartResponse(portfolio, period, interval, Collections.emptyList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioChartResponse getClientPortfolioChartFromDatabase(Map<String, Double> portfolio, String period) {
        try {
            log.debug("Fetching client portfolio chart from database (MANUAL source only) for period: {}", period);
            
            if (portfolio == null || portfolio.isEmpty()) {
                log.warn("Empty portfolio provided for client chart");
                return new PortfolioChartResponse(portfolio, period, "1wk", Collections.emptyList());
            }
            
            // For 6-month period, use 1-week intervals
            String interval = "1wk";
            if (period.equalsIgnoreCase("6mo") || period.equalsIgnoreCase("6m")) {
                interval = "1wk";
            } else if (period.equalsIgnoreCase("1d")) {
                interval = "15m";
            } else if (period.equalsIgnoreCase("1w") || period.equalsIgnoreCase("5d")) {
                interval = "60m";
            } else if (period.equalsIgnoreCase("1mo") || period.equalsIgnoreCase("1m")) {
                interval = "1d";
            } else if (period.equalsIgnoreCase("1y")) {
                interval = "1wk";
            } else if (period.equalsIgnoreCase("5y")) {
                interval = "1mo";
            }
            
            // Calculate date range based on period
            Instant endDate = Instant.now();
            LocalDate endLocalDate = endDate.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate startLocalDate;
            
            switch (period.toLowerCase()) {
                case "6mo":
                case "6m":
                    startLocalDate = endLocalDate.minusMonths(6);
                    break;
                case "1y":
                    startLocalDate = endLocalDate.minusYears(1);
                    break;
                case "1mo":
                case "1m":
                    startLocalDate = endLocalDate.minusMonths(1);
                    break;
                case "1w":
                case "5d":
                    startLocalDate = endLocalDate.minusWeeks(1);
                    break;
                case "1d":
                    startLocalDate = endLocalDate.minusDays(1);
                    break;
                case "5y":
                    startLocalDate = endLocalDate.minusYears(5);
                    break;
                default:
                    startLocalDate = endLocalDate.minusMonths(6); // Default to 6 months
            }
            
            Instant startDate = startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            log.debug("Date range: {} to {}, interval: {}", startDate, endDate, interval);
            
            // Get today's date (without time) for comparison
            LocalDate today = LocalDate.now();
            
            // Map to store aggregated portfolio values by date
            Map<LocalDate, Double> portfolioValuesByDate = new TreeMap<>();
            
            // Fetch historical prices for each symbol from database (MANUAL source only)
            for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
                String symbol = entry.getKey().toUpperCase();
                Double quantity = entry.getValue();
                
                if (symbol == null || symbol.isEmpty() || quantity == null || quantity <= 0) {
                    continue;
                }
                
                // Fetch historical prices from database filtered by MANUAL source
                List<AssetPrice> historicalPrices = assetPriceRepository.findBySymbolAndDateRangeManual(
                        symbol, startDate, endDate);
                
                log.debug("Found {} MANUAL historical prices for symbol {}", historicalPrices.size(), symbol);
                
                // Aggregate prices by date: price * quantity
                for (AssetPrice price : historicalPrices) {
                    LocalDate priceDate = price.getPriceDate()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    
                    // Skip today's date - we'll use live price for today
                    if (priceDate.equals(today)) {
                        continue;
                    }
                    
                    // Calculate value: price * quantity
                    double value = price.getCurrentPrice().doubleValue() * quantity;
                    portfolioValuesByDate.merge(priceDate, value, Double::sum);
                }
            }
            
            // Fetch live prices for today only
            Map<String, Double> todayPrices = new HashMap<>();
            for (String symbol : portfolio.keySet()) {
                try {
                    PriceResponse priceResponse = getCurrentPriceBySymbol(symbol);
                    if (priceResponse != null && priceResponse.getPrice() != null) {
                        BigDecimal livePrice = BigDecimal.valueOf(priceResponse.getPrice());
                        if (livePrice.compareTo(BigDecimal.ZERO) > 0) {
                            todayPrices.put(symbol, livePrice.doubleValue());
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch live price for {}: {}", symbol, e.getMessage());
                }
            }
            
            // Add today's aggregated value (sum of all assets: price * quantity)
            if (!todayPrices.isEmpty()) {
                double todayValue = todayPrices.entrySet().stream()
                        .mapToDouble(e -> e.getValue() * portfolio.get(e.getKey()))
                        .sum();
                portfolioValuesByDate.put(today, todayValue);
            }
            
            // For 6-month period with 1-week intervals, aggregate data weekly
            // Group by week and use the last value of each week (typically Friday or last trading day)
            if ((period.equalsIgnoreCase("6mo") || period.equalsIgnoreCase("6m")) && interval.equals("1wk")) {
                Map<LocalDate, Double> weeklyAggregated = new TreeMap<>();
                
                // Group by week start (Monday) and keep the last value of each week
                for (Map.Entry<LocalDate, Double> entry : portfolioValuesByDate.entrySet()) {
                    LocalDate weekStart = entry.getKey().with(java.time.DayOfWeek.MONDAY);
                    // Use the actual date (not week start) so we get the last value of the week
                    // TreeMap will naturally keep the last entry for each week when we iterate
                    weeklyAggregated.put(entry.getKey(), entry.getValue());
                }
                
                // Now group by week and keep only the last day of each week
                Map<LocalDate, Double> finalWeekly = new TreeMap<>();
                LocalDate currentWeekStart = null;
                LocalDate lastDateInWeek = null;
                Double lastValueInWeek = null;
                
                for (Map.Entry<LocalDate, Double> entry : weeklyAggregated.entrySet()) {
                    LocalDate weekStart = entry.getKey().with(java.time.DayOfWeek.MONDAY);
                    
                    if (currentWeekStart == null || !currentWeekStart.equals(weekStart)) {
                        // Save previous week's last value
                        if (currentWeekStart != null && lastDateInWeek != null) {
                            finalWeekly.put(lastDateInWeek, lastValueInWeek);
                        }
                        // Start new week
                        currentWeekStart = weekStart;
                        lastDateInWeek = entry.getKey();
                        lastValueInWeek = entry.getValue();
                    } else {
                        // Same week, update to latest date
                        lastDateInWeek = entry.getKey();
                        lastValueInWeek = entry.getValue();
                    }
                }
                
                // Don't forget the last week
                if (lastDateInWeek != null) {
                    finalWeekly.put(lastDateInWeek, lastValueInWeek);
                }
                
                portfolioValuesByDate = finalWeekly;
            }
            
            // Convert to PortfolioChartDataPoint list
            List<com.app.portfolio.dto.pricing.PortfolioChartDataPoint> chartData = 
                    portfolioValuesByDate.entrySet().stream()
                            .map(entry -> {
                                com.app.portfolio.dto.pricing.PortfolioChartDataPoint point = 
                                        new com.app.portfolio.dto.pricing.PortfolioChartDataPoint();
                                point.setTime(entry.getKey().atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toString());
                                point.setValue(entry.getValue());
                                return point;
                            })
                            .collect(Collectors.toList());
            
            log.debug("Generated {} data points for client portfolio chart", chartData.size());
            
            return new PortfolioChartResponse(portfolio, period, interval, chartData);
                    
        } catch (Exception e) {
            log.error("Error fetching client portfolio chart from database: {}", e.getMessage(), e);
            return new PortfolioChartResponse(portfolio, period, "1wk", Collections.emptyList());
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
     * Migration method: Populate symbol column in existing asset_prices records
     * This should be called once after adding the symbol column to preserve existing price data
     */
    @Transactional
    public void migrateAssetPricesWithSymbols() {
        log.info("Starting migration: Populating symbols in asset_prices table");
        
        List<AssetPrice> pricesWithoutSymbol = assetPriceRepository.findAllWithoutSymbol();
        log.info("Found {} asset prices without symbols", pricesWithoutSymbol.size());
        
        if (pricesWithoutSymbol.isEmpty()) {
            log.info("No prices need migration - all prices already have symbols");
            return;
        }
        
        int updatedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (AssetPrice price : pricesWithoutSymbol) {
            try {
                // Load asset to get symbol (handles lazy loading)
                Asset asset = price.getAsset();
                if (asset != null) {
                    String symbol = asset.getSymbol();
                    if (symbol != null && !symbol.trim().isEmpty()) {
                        symbol = symbol.toUpperCase().trim();
                        price.setSymbol(symbol);
                        assetPriceRepository.save(price);
                        updatedCount++;
                        
                        if (updatedCount % 100 == 0) {
                            log.info("Migrated {} prices so far...", updatedCount);
                        }
                    } else {
                        skippedCount++;
                        log.debug("Skipping price ID {} - asset symbol is empty", price.getId());
                    }
                } else {
                    skippedCount++;
                    log.warn("Skipping price ID {} - asset is null (may have been deleted)", price.getId());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Error migrating price ID {}: {}", price.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Migration completed: Updated {} prices, Skipped {} prices, Errors: {}", 
                updatedCount, skippedCount, errorCount);
        
        if (updatedCount > 0) {
            log.info("Successfully migrated {} asset prices to use symbol-based pricing", updatedCount);
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
