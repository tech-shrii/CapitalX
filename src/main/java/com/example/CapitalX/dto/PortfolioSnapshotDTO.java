package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshotDTO {
    private Long uploadId;
    private String periodLabel;
    private String periodType;
    private LocalDateTime uploadDate;
    private String fileName;
    private BigDecimal totalInvestedValue;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalProfitLoss;
    private Integer numberOfAssets;
    private Integer numberOfProfitableAssets;
    private Integer numberOfLossAssets;
}
