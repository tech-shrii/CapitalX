package com.app.portfolio.service.asset;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.AssetMapper;
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
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final ClientRepository clientRepository;
    private final AssetMapper assetMapper;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByClientId(Long clientId, Long userId) {
        if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }
        return assetRepository.findByClientIdOrderByPurchaseDateDesc(clientId)
                .stream()
                .map(asset -> {
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                    BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
                    return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
        return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
    }

    @Override
    @Transactional
    public AssetResponse createAsset(Long clientId, AssetRequest request, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }

        Asset asset = assetMapper.toEntity(request, null);
        asset.setClient(client);
        asset = assetRepository.save(asset);
        return assetMapper.toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(Long id, AssetRequest request, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        asset = assetMapper.toEntity(request, asset);
        asset = assetRepository.save(asset);
        BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
        return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id, Long userId) {
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        assetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PnlResponse calculatePnL(Long clientId, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }

        List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateDesc(clientId);
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        List<AssetResponse> assetResponses = assets.stream()
                .map(asset -> {
                    BigDecimal currentPrice = pricingService.getCurrentPrice(asset.getId());
                    BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
                    return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
                })
                .collect(Collectors.toList());

        for (Asset asset : assets) {
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

        return PnlResponse.builder()
                .clientId(clientId)
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .assets(assetResponses)
                .build();
    }

    private BigDecimal calculateProfitLoss(Asset asset, BigDecimal currentPrice) {
        if (currentPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
        BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
        return currentValue.subtract(invested);
    }

    private BigDecimal calculateProfitLossPercent(Asset asset, BigDecimal currentPrice) {
        if (currentPrice == null || asset.getBuyingRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
        return invested.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }
}
