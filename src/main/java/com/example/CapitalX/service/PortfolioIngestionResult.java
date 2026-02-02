package com.example.CapitalX.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for portfolio ingestion result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioIngestionResult {
    private Long uploadId;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String periodLabel;
    private Integer holdingsCount;
    private String message;
    private boolean success;
}
