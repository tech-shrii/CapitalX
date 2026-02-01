package com.example.CapitalX.mapper;

import com.example.CapitalX.beans.PortfolioHolding;
import com.example.CapitalX.dto.AssetPerformanceDTO;

import java.math.BigDecimal;

public class AssetPerformanceMapper {

    public static AssetPerformanceDTO convertBeanToDTO(PortfolioHolding holding) {
        if (holding == null) {
            return null;
        }

        AssetPerformanceDTO dto = new AssetPerformanceDTO();
        dto.setHoldingId(holding.getHoldingId());
        dto.setAssetId(holding.getAsset().getAssetId());
        dto.setAssetName(holding.getAsset().getAssetName());
        dto.setAssetType(holding.getAsset().getAssetType());
        dto.setQuantity(holding.getQuantity());
        dto.setPurchasePrice(holding.getBuyPrice());
        dto.setCurrentPrice(holding.getCurrentPrice());
        dto.setTotalCost(holding.getInvestedValue());
        dto.setCurrentValue(holding.getCurrentValue());
        dto.setGainLoss(holding.getProfitLoss());

        // Calculate profit/loss percentage
        if (holding.getInvestedValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal gainLossPercentage = holding.getProfitLoss()
                .divide(holding.getInvestedValue(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            dto.setGainLossPercentage(gainLossPercentage);
        }

        // Set dates
        dto.setPurchaseDate(holding.getInvestmentStartDate());
        dto.setExitDate(holding.getInvestmentEndDate());

        // Determine investment status
        String status = holding.getInvestmentEndDate() == null ? "ACTIVE" : "EXITED";
        dto.setStatus(status);

        return dto;
    }
}
