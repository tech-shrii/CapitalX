package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalyticsDTO {
    private BigDecimal totalPortfolioValue;
    private BigDecimal totalInvestedValue;
    private BigDecimal totalProfitLoss;
    private Long numberOfCustomers;
    private Long numberOfAssets;
    private List<CustomerPerformanceDTO> topPerformingCustomers;
    private List<CustomerPerformanceDTO> bottomPerformingCustomers;
    private List<AssetExposureDTO> topAssets;
    private List<AssetExposureDTO> riskAssets;
}
