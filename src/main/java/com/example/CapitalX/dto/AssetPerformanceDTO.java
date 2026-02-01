package com.example.CapitalX.dto;

import com.example.CapitalX.beans.Asset.AssetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetPerformanceDTO {
    private Long holdingId;
    private Long assetId;
    private String assetName;
    private AssetType assetType;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal totalCost;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPercentage;
    private LocalDate purchaseDate;
    private LocalDate exitDate;
    private String status;
}
