package com.app.portfolio.dto.asset;

import com.app.portfolio.beans.Asset;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class AssetResponse {

    private Long id;
    private Long clientId;
    private String name;
    private Asset.AssetCategory category;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal buyingRate;
    private LocalDate purchaseDate;
    private String currency;
    private BigDecimal currentPrice;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
    private Instant createdAt;
    private Instant updatedAt;
}
