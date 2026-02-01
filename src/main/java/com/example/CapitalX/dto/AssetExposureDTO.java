package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetExposureDTO {
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String assetType;
    private BigDecimal totalValue;
    private Long numberOfCustomersHolding;
    private BigDecimal averageReturn;
    private String riskLevel; // LOW, MEDIUM, HIGH
}
