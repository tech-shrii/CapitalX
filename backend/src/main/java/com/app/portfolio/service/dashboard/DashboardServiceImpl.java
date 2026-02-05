package com.app.portfolio.service.dashboard;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetPriceRepository;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;
    private final PricingService pricingService;
    private final AssetPriceRepository assetPriceRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        log.debug("Getting dashboard summary for userId: {}", userId);
        try {
            List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.debug("Found {} clients for userId: {}", clients.size(), userId);
            
            List<Asset> allAssets = clients.stream()
                    .flatMap(client -> assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId()).stream())
                    .collect(Collectors.toList());
            log.debug("Found {} total assets across all clients", allAssets.size());

            List<Asset> unsoldAssets = allAssets.stream().filter(a -> !a.isSold()).collect(Collectors.toList());

            // --- Daily P/L Calculation Setup ---
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            List<AssetPrice> openPrices = assetPriceRepository.findBySourceAndPriceDateBetween(AssetPrice.PriceSource.OPEN, startOfDay, endOfDay);
            Map<String, BigDecimal> openPriceMap = openPrices.stream()
                .collect(Collectors.toMap(AssetPrice::getSymbol, AssetPrice::getCurrentPrice, (p1, p2) -> p1));
            
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            BigDecimal totalOpenValue = BigDecimal.ZERO;
            Map<String, BigDecimal> assetAllocation = new HashMap<>();
            Map<String, BigDecimal> categoryBreakdown = new HashMap<>();

            for (Asset asset : allAssets) {
                BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                totalInvested = totalInvested.add(invested);
                
                if (asset.isSold()) {
                    if(asset.getSellingRate() != null) {
                        totalCurrentValue = totalCurrentValue.add(asset.getSellingRate().multiply(asset.getQuantity()));
                    }
                } else {
                    BigDecimal currentPrice = (asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                            ? pricingService.getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol())
                            : pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate();
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    totalCurrentValue = totalCurrentValue.add(currentValue);

                    // Daily P/L calculation
                    BigDecimal openPrice = openPriceMap.get(asset.getSymbol().toUpperCase());
                    if (openPrice != null) {
                        totalOpenValue = totalOpenValue.add(openPrice.multiply(asset.getQuantity()));
                    } else {
                        // Fallback: If no open price, assume it opened at its current value to not skew P/L negatively.
                        totalOpenValue = totalOpenValue.add(currentValue);
                    }

                    String category = asset.getCategory().name();
                    assetAllocation.put(category, assetAllocation.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                    categoryBreakdown.put(category, categoryBreakdown.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                }
            }

            Integer assetCategoryCount = assetAllocation.keySet().size();
            
            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
            BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            
            // --- Finalize Daily P/L Calculation ---
            BigDecimal dailyProfitLoss = totalCurrentValue.subtract(totalOpenValue);
            BigDecimal dailyProfitLossPercentage = totalOpenValue.compareTo(BigDecimal.ZERO) > 0
                ? dailyProfitLoss.divide(totalOpenValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

            List<DashboardSummaryResponse.ClientSummaryDto> recentClients = clients.stream()
                    .limit(5)
                    .map(client -> {
                        List<Asset> clientAssets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId());
                        BigDecimal clientPnL = BigDecimal.ZERO;
                        for (Asset asset : clientAssets) {
                            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                            BigDecimal finalPrice = asset.isSold() ? asset.getSellingRate() 
                                    : (asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                                    ? pricingService.getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol())
                                    : pricingService.getCurrentPrice(asset.getId());
                            if (finalPrice == null) {
                                finalPrice = asset.getBuyingRate();
                            }
                            BigDecimal finalValue = finalPrice.multiply(asset.getQuantity());
                            clientPnL = clientPnL.add(finalValue.subtract(invested));
                        }
                        return DashboardSummaryResponse.ClientSummaryDto.builder()
                                .id(client.getId())
                                .name(client.getName())
                                .email(client.getEmail())
                                .assetCount((long) clientAssets.size())
                                .profitLoss(clientPnL)
                                .build();
                    })
                    .collect(Collectors.toList());

            List<DashboardSummaryResponse.TopAssetDto> allAssetsWithReturns = unsoldAssets.stream()
                .map(asset -> {
                    BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                    BigDecimal currentPrice = (asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                            ? pricingService.getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol())
                            : pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate();
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    BigDecimal returns = invested.compareTo(BigDecimal.ZERO) > 0
                            ? currentValue.subtract(invested).divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return DashboardSummaryResponse.TopAssetDto.builder()
                            .name(asset.getName())
                            .category(asset.getCategory().name())
                            .currentValue(currentValue)
                            .returns(returns)
                            .build();
                })
                .collect(Collectors.toList());

            List<DashboardSummaryResponse.TopAssetDto> topAssets = allAssetsWithReturns.stream()
                .sorted((a, b) -> b.getReturns().compareTo(a.getReturns()))
                .limit(5)
                .collect(Collectors.toList());

            List<DashboardSummaryResponse.TopAssetDto> worstAssets = allAssetsWithReturns.stream()
                .sorted(Comparator.comparing(DashboardSummaryResponse.TopAssetDto::getReturns))
                .limit(5)
                .collect(Collectors.toList());

            Map<String, Double> portfolioMap = unsoldAssets.stream()
                .filter(asset -> asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                .collect(Collectors.toMap(
                    Asset::getSymbol,
                    asset -> asset.getQuantity().doubleValue(),
                    Double::sum
                ));
                
            // Calculate today's P&L using database data instead of external pricing service
            BigDecimal todaysPL = BigDecimal.ZERO;
            BigDecimal todaysPLPercentage = BigDecimal.ZERO;
            
            // Get today's performance from database (1d period)
            com.app.portfolio.dto.pricing.PortfolioChartResponse todaysPerformance = 
                pricingService.getPortfolioChartFromDatabase(portfolioMap, "1d", "15m");
            
            if (todaysPerformance != null && todaysPerformance.getData() != null && todaysPerformance.getData().size() > 1) {
                var startValue = todaysPerformance.getData().get(0).getValue();
                var endValue = todaysPerformance.getData().get(todaysPerformance.getData().size() - 1).getValue();
                if(startValue != null && endValue != null) {
                    todaysPL = BigDecimal.valueOf(endValue - startValue);
                    if (startValue != 0) {
                        todaysPLPercentage = todaysPL.divide(BigDecimal.valueOf(startValue), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    }
                }
            }


            List<String> labels;
            List<BigDecimal> performanceData;

            if (!portfolioMap.isEmpty()) {
                log.debug("Fetching portfolio chart for {} symbols: {}", portfolioMap.size(), portfolioMap.keySet());
                com.app.portfolio.dto.pricing.PortfolioChartResponse portfolioChart =
                    pricingService.getPortfolioChartFromDatabase(portfolioMap, "6mo", "1wk");

                if (portfolioChart != null && portfolioChart.getData() != null && !portfolioChart.getData().isEmpty()) {
                    log.debug("Portfolio chart returned {} data points", portfolioChart.getData().size());
                    labels = portfolioChart.getData().stream()
                                    .map(com.app.portfolio.dto.pricing.PortfolioChartDataPoint::getTime)
                                    .collect(Collectors.toList());
                    performanceData = portfolioChart.getData().stream()
                                    .map(p -> BigDecimal.valueOf(p.getValue()))
                                    .collect(Collectors.toList());
                    log.debug("Processed {} labels and {} performance data points", labels.size(), performanceData.size());
                } else {
                    log.warn("Portfolio chart returned empty data. portfolioChart={}, data={}", 
                            portfolioChart, portfolioChart != null ? portfolioChart.getData() : "null");
                    labels = Arrays.asList("Today");
                    performanceData = Arrays.asList(totalCurrentValue);
                }
            } else {
                log.debug("Portfolio map is empty, no chart data");
                labels = Collections.emptyList();
                performanceData = Collections.emptyList();
            }
        
            DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                    .totalClients((long) clients.size())
                    .totalAssets((long) allAssets.size())
                    .totalInvested(totalInvested)
                    .totalCurrentValue(totalCurrentValue)
                    .totalProfitLoss(totalProfitLoss)
                    .totalProfitLossPercent(totalProfitLossPercent)
                    .dailyProfitLoss(dailyProfitLoss)
                    .dailyProfitLossPercentage(dailyProfitLossPercentage)
                    .recentClients(recentClients)
                    .assetAllocation(assetAllocation)
                    .portfolioPerformance(DashboardSummaryResponse.PortfolioPerformanceData.builder()
                            .labels(labels)
                            .data(performanceData)
                            .build())
                    .topAssets(topAssets)
                    .worstAssets(worstAssets)
                    .assetCategoryCount(assetCategoryCount)
                    .todaysPL(todaysPL)
                    .todaysPLPercentage(todaysPLPercentage)
                    .assetCategoryBreakdown(categoryBreakdown)
                    .build();
            
            log.debug("Dashboard summary built successfully for userId: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("Error building dashboard summary for userId: {}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getClientDashboardSummary(Long clientId, Long userId) {
        log.debug("Getting client dashboard summary for clientId: {}, userId: {}", clientId, userId);
        try {
            // Verify client belongs to user
            if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
                log.warn("Client {} does not belong to user {}", clientId, userId);
                throw new ResourceNotFoundException("Client", clientId);
            }

            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
            
            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(clientId);
            log.debug("Found {} assets for clientId: {}", assets.size(), clientId);

            List<Asset> unsoldAssets = assets.stream().filter(a -> !a.isSold()).collect(Collectors.toList());

            // --- Daily P/L Calculation Setup ---
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            List<AssetPrice> openPrices = assetPriceRepository.findBySourceAndPriceDateBetween(AssetPrice.PriceSource.OPEN, startOfDay, endOfDay);
            Map<String, BigDecimal> openPriceMap = openPrices.stream()
                .collect(Collectors.toMap(AssetPrice::getSymbol, AssetPrice::getCurrentPrice, (p1, p2) -> p1));

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            BigDecimal totalOpenValue = BigDecimal.ZERO;
            Map<String, BigDecimal> assetAllocation = new HashMap<>();
            Map<String, BigDecimal> categoryBreakdown = new HashMap<>();

            for (Asset asset : assets) {
                BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                totalInvested = totalInvested.add(invested);

                if(asset.isSold()){
                    if(asset.getSellingRate() != null){
                        totalCurrentValue = totalCurrentValue.add(asset.getSellingRate().multiply(asset.getQuantity()));
                    }
                } else {
                    BigDecimal currentPrice = (asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                            ? pricingService.getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol())
                            : pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate();
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    totalCurrentValue = totalCurrentValue.add(currentValue);
                    
                    // Daily P/L calculation
                    BigDecimal openPrice = openPriceMap.get(asset.getSymbol().toUpperCase());
                    if (openPrice != null) {
                        totalOpenValue = totalOpenValue.add(openPrice.multiply(asset.getQuantity()));
                    } else {
                        // Fallback: If no open price, assume it opened at its current value.
                        totalOpenValue = totalOpenValue.add(currentValue);
                    }
                    
                    String category = asset.getCategory().name();
                    assetAllocation.put(category, assetAllocation.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                    categoryBreakdown.put(category, categoryBreakdown.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                }
            }

            Integer assetCategoryCount = assetAllocation.keySet().size();

            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
            BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // --- Finalize Daily P/L Calculation ---
            BigDecimal dailyProfitLoss = totalCurrentValue.subtract(totalOpenValue);
            BigDecimal dailyProfitLossPercentage = totalOpenValue.compareTo(BigDecimal.ZERO) > 0
                ? dailyProfitLoss.divide(totalOpenValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

            List<DashboardSummaryResponse.TopAssetDto> allAssetsWithReturns = unsoldAssets.stream()
                .map(asset -> {
                    BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                    BigDecimal currentPrice = (asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                            ? pricingService.getCurrentPriceBySymbolAsBigDecimal(asset.getSymbol())
                            : pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate();
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    BigDecimal returns = invested.compareTo(BigDecimal.ZERO) > 0
                            ? currentValue.subtract(invested).divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return DashboardSummaryResponse.TopAssetDto.builder()
                            .name(asset.getName())
                            .category(asset.getCategory().name())
                            .currentValue(currentValue)
                            .returns(returns)
                            .build();
                })
                .collect(Collectors.toList());

            List<DashboardSummaryResponse.TopAssetDto> topAssets = allAssetsWithReturns.stream()
                .sorted((a, b) -> b.getReturns().compareTo(a.getReturns()))
                .limit(5)
                .collect(Collectors.toList());
            
            List<DashboardSummaryResponse.TopAssetDto> worstAssets = allAssetsWithReturns.stream()
                .sorted(Comparator.comparing(DashboardSummaryResponse.TopAssetDto::getReturns))
                .limit(5)
                .collect(Collectors.toList());

            Map<String, Double> portfolioMap = unsoldAssets.stream()
                .filter(asset -> asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                .collect(Collectors.toMap(
                    Asset::getSymbol,
                    asset -> asset.getQuantity().doubleValue(),
                    Double::sum
                ));
            
            // Calculate today's P&L using database data instead of external pricing service
            BigDecimal todaysPL = BigDecimal.ZERO;
            BigDecimal todaysPLPercentage = BigDecimal.ZERO;
            
            // Get today's performance from database (1d period)
            com.app.portfolio.dto.pricing.PortfolioChartResponse todaysPerformance = 
                pricingService.getPortfolioChartFromDatabase(portfolioMap, "1d", "15m");
            
            if (todaysPerformance != null && todaysPerformance.getData() != null && todaysPerformance.getData().size() > 1) {
                var startValue = todaysPerformance.getData().get(0).getValue();
                var endValue = todaysPerformance.getData().get(todaysPerformance.getData().size() - 1).getValue();
                if(startValue != null && endValue != null) {
                    todaysPL = BigDecimal.valueOf(endValue - startValue);
                    if (startValue != 0) {
                        todaysPLPercentage = todaysPL.divide(BigDecimal.valueOf(startValue), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    }
                }
            }


            List<String> labels;
            List<BigDecimal> performanceData;
            
            if (!portfolioMap.isEmpty()) {
                com.app.portfolio.dto.pricing.PortfolioChartResponse portfolioChart = 
                    pricingService.getPortfolioChartFromDatabase(portfolioMap, "6mo", "1wk");

                if (portfolioChart != null && portfolioChart.getData() != null && !portfolioChart.getData().isEmpty()) {
                    labels = portfolioChart.getData().stream()
                                    .map(com.app.portfolio.dto.pricing.PortfolioChartDataPoint::getTime)
                                    .collect(Collectors.toList());
                    performanceData = portfolioChart.getData().stream()
                                    .map(p -> BigDecimal.valueOf(p.getValue()))
                                    .collect(Collectors.toList());
                } else {
                    labels = Arrays.asList("Today");
                    performanceData = Arrays.asList(totalCurrentValue);
                }
            } else {
                labels = Collections.emptyList();
                performanceData = Collections.emptyList();
            }

            DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                    .totalClients(1L)
                    .totalAssets((long) assets.size())
                    .totalInvested(totalInvested)
                    .totalCurrentValue(totalCurrentValue)
                    .totalProfitLoss(totalProfitLoss)
                    .totalProfitLossPercent(totalProfitLossPercent)
                    .dailyProfitLoss(dailyProfitLoss)
                    .dailyProfitLossPercentage(dailyProfitLossPercentage)
                    .recentClients(Collections.emptyList())
                    .assetAllocation(assetAllocation)
                    .portfolioPerformance(DashboardSummaryResponse.PortfolioPerformanceData.builder()
                            .labels(labels)
                            .data(performanceData)
                            .build())
                    .topAssets(topAssets)
                    .worstAssets(worstAssets)
                    .assetCategoryCount(assetCategoryCount)
                    .todaysPL(todaysPL)
                    .todaysPLPercentage(todaysPLPercentage)
                    .assetCategoryBreakdown(categoryBreakdown)
                    .build();
            
            log.debug("Client dashboard summary built successfully for clientId: {}, userId: {}", clientId, userId);
            return response;
        } catch (Exception e) {
            log.error("Error building client dashboard summary for clientId: {}, userId: {}", clientId, userId, e);
            throw e;
        }
    }
}
