package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPerformanceDTO {
    private Long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal portfolioValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal returnPercentage;
    private Integer numberOfAssets;
}
