package com.app.portfolio.dto.asset;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PnlResponse {

    private Long clientId;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private List<AssetResponse> assets;
}
