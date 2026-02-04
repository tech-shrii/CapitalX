package com.app.portfolio.service.dashboard;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;
    private final PricingService pricingService;

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

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
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
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate(); // Fallback to buying rate
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    totalCurrentValue = totalCurrentValue.add(currentValue);

                    // Asset allocation for unsold assets
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

            List<DashboardSummaryResponse.ClientSummaryDto> recentClients = clients.stream()
                    .limit(5)
                    .map(client -> {
                        List<Asset> clientAssets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId());
                        BigDecimal clientPnL = BigDecimal.ZERO;
                        for (Asset asset : clientAssets) {
                            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                            BigDecimal finalPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
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
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
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

            // Portfolio performance for unsold assets
            Map<String, Double> portfolioMap = unsoldAssets.stream()
                .filter(asset -> asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                .collect(Collectors.toMap(
                    Asset::getSymbol,
                    asset -> asset.getQuantity().doubleValue(),
                    Double::sum
                ));
                
            com.app.portfolio.dto.pricing.PortfolioChartResponse todaysPerformance = pricingService.getPortfolioChart(portfolioMap, "1d", "1d");
            BigDecimal todaysPL = BigDecimal.ZERO;
            BigDecimal todaysPLPercentage = BigDecimal.ZERO;

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
                    pricingService.getPortfolioChart(portfolioMap, "6m", "1wk");

                if (portfolioChart != null && portfolioChart.getData() != null && !portfolioChart.getData().isEmpty()) {
                    labels = portfolioChart.getData().stream()
                                    .map(com.app.portfolio.dto.pricing.PortfolioChartDataPoint::getTime)
                                    .collect(Collectors.toList());
                    performanceData = portfolioChart.getData().stream()
                                    .map(p -> BigDecimal.valueOf(p.getValue()))
                                    .collect(Collectors.toList());
                } else {
                    // Fallback to simplified data
                    labels = Arrays.asList("Today");
                    performanceData = Arrays.asList(totalCurrentValue);
                }
            } else {
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

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
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
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                    if (currentPrice == null) {
                        currentPrice = asset.getBuyingRate(); // Fallback to buying rate
                    }
                    BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                    totalCurrentValue = totalCurrentValue.add(currentValue);
                    
                    // Asset allocation by category
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

            List<DashboardSummaryResponse.TopAssetDto> allAssetsWithReturns = unsoldAssets.stream()
                .map(asset -> {
                    BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
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

            // Portfolio performance
            Map<String, Double> portfolioMap = unsoldAssets.stream()
                .filter(asset -> asset.getSymbol() != null && !asset.getSymbol().isEmpty())
                .collect(Collectors.toMap(
                    Asset::getSymbol,
                    asset -> asset.getQuantity().doubleValue(),
                    Double::sum
                ));
            
            com.app.portfolio.dto.pricing.PortfolioChartResponse todaysPerformance = pricingService.getPortfolioChart(portfolioMap, "1d", "1d");
            BigDecimal todaysPL = BigDecimal.ZERO;
            BigDecimal todaysPLPercentage = BigDecimal.ZERO;
            
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
                    pricingService.getPortfolioChart(portfolioMap, "6m", "1wk");

                if (portfolioChart != null && portfolioChart.getData() != null && !portfolioChart.getData().isEmpty()) {
                    labels = portfolioChart.getData().stream()
                                    .map(com.app.portfolio.dto.pricing.PortfolioChartDataPoint::getTime)
                                    .collect(Collectors.toList());
                    performanceData = portfolioChart.getData().stream()
                                    .map(p -> BigDecimal.valueOf(p.getValue()))
                                    .collect(Collectors.toList());
                } else {
                    // Fallback to simplified data
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
