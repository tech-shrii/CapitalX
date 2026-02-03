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
                    .flatMap(client -> assetRepository.findByClientIdOrderByPurchaseDateDesc(client.getId()).stream())
                    .collect(Collectors.toList());
            log.debug("Found {} total assets across all clients", allAssets.size());

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            Map<String, BigDecimal> assetAllocation = new HashMap<>();
            Map<String, BigDecimal> categoryBreakdown = new HashMap<>();

            for (Asset asset : allAssets) {
                BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                if (currentPrice == null) {
                    currentPrice = asset.getBuyingRate(); // Fallback to buying rate
                }
                BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                totalInvested = totalInvested.add(invested);
                totalCurrentValue = totalCurrentValue.add(currentValue);
                
                // Asset allocation by category
                String category = asset.getCategory().name();
                assetAllocation.put(category, assetAllocation.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                categoryBreakdown.put(category, categoryBreakdown.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
            }

            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
            BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            List<DashboardSummaryResponse.ClientSummaryDto> recentClients = clients.stream()
                    .limit(5)
                    .map(client -> {
                        List<Asset> clientAssets = assetRepository.findByClientIdOrderByPurchaseDateDesc(client.getId());
                        BigDecimal clientPnL = BigDecimal.ZERO;
                        for (Asset asset : clientAssets) {
                            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                            BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                            if (currentPrice == null) {
                                currentPrice = asset.getBuyingRate();
                            }
                            BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                            clientPnL = clientPnL.add(currentValue.subtract(invested));
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

            // Top performing assets
            List<DashboardSummaryResponse.TopAssetDto> topAssets = allAssets.stream()
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
                    .sorted((a, b) -> b.getReturns().compareTo(a.getReturns()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Portfolio performance (simplified - just current value for now)
            List<String> labels = Arrays.asList("Today");
            List<BigDecimal> performanceData = Arrays.asList(totalCurrentValue);

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
            
            List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateDesc(clientId);
            log.debug("Found {} assets for clientId: {}", assets.size(), clientId);

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrentValue = BigDecimal.ZERO;
            Map<String, BigDecimal> assetAllocation = new HashMap<>();
            Map<String, BigDecimal> categoryBreakdown = new HashMap<>();

            for (Asset asset : assets) {
                BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
                BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                if (currentPrice == null) {
                    currentPrice = asset.getBuyingRate(); // Fallback to buying rate
                }
                BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
                totalInvested = totalInvested.add(invested);
                totalCurrentValue = totalCurrentValue.add(currentValue);
                
                // Asset allocation by category
                String category = asset.getCategory().name();
                assetAllocation.put(category, assetAllocation.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
                categoryBreakdown.put(category, categoryBreakdown.getOrDefault(category, BigDecimal.ZERO).add(currentValue));
            }

            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
            BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // Top performing assets
            List<DashboardSummaryResponse.TopAssetDto> topAssets = assets.stream()
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
                    .sorted((a, b) -> b.getReturns().compareTo(a.getReturns()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Portfolio performance (simplified)
            List<String> labels = Arrays.asList("Today");
            List<BigDecimal> performanceData = Arrays.asList(totalCurrentValue);

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
