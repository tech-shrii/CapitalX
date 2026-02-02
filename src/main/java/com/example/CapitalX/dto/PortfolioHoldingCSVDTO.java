package com.example.CapitalX.dto;

import com.example.CapitalX.beans.Asset;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO to map CSV row to a portfolio holding entry
 * Corresponds to portfolio_holdings table columns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHoldingCSVDTO {
    private String assetCode;
    private String assetName;
    private Asset.AssetType assetType;
    private String exchangeOrMarket;
    private BigDecimal quantity;
    private BigDecimal buyPrice;
    private BigDecimal currentPrice;
    private BigDecimal investedValue;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private LocalDate investmentStartDate;
    private LocalDate investmentEndDate;
}
