package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualReportDTO {
    private Long annualPerfId;
    private Integer financialYear;
    private BigDecimal openingValue;
    private BigDecimal closingValue;
    private BigDecimal totalInvestedDuringYear;
    private BigDecimal totalProfitLoss;
    private BigDecimal returnPercentage;
    private String bestPerformingAsset;
    private String worstPerformingAsset;
}
