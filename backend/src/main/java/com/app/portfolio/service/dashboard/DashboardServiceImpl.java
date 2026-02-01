package com.app.portfolio.service.dashboard;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ClientRepository clientRepository;
    private final AssetRepository assetRepository;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Asset> allAssets = clients.stream()
                .flatMap(client -> assetRepository.findByClientIdOrderByPurchaseDateDesc(client.getId()).stream())
                .collect(Collectors.toList());

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        for (Asset asset : allAssets) {
            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
            BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
            BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
            totalInvested = totalInvested.add(invested);
            totalCurrentValue = totalCurrentValue.add(currentValue);
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

        return DashboardSummaryResponse.builder()
                .totalClients((long) clients.size())
                .totalAssets((long) allAssets.size())
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .recentClients(recentClients)
                .build();
    }
}
