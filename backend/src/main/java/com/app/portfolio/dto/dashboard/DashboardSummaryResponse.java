package com.app.portfolio.dto.dashboard;

import com.app.portfolio.dto.client.ClientResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {

    private Long totalClients;
    private Long totalAssets;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private List<ClientSummaryDto> recentClients;

    @Data
    @Builder
    public static class ClientSummaryDto {
        private Long id;
        private String name;
        private String email;
        private Long assetCount;
        private BigDecimal profitLoss;
    }
}
