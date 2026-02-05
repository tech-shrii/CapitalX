package com.app.portfolio.dto.dashboard;

import com.app.portfolio.dto.client.ClientResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    private Integer assetCategoryCount;
    private BigDecimal todaysPL;
    private BigDecimal todaysPLPercentage;
    private BigDecimal dailyProfitLoss;
    private BigDecimal dailyProfitLossPercentage;
    
    // Additional fields for frontend charts
    private Map<String, BigDecimal> assetAllocation; // Category -> Value
    private PortfolioPerformanceData portfolioPerformance; // Historical data
    private List<TopAssetDto> topAssets; // Top performing assets
    private List<TopAssetDto> worstAssets; // Worst performing assets
    private Map<String, BigDecimal> assetCategoryBreakdown; // Category -> Value

    @Data
    @Builder
    public static class ClientSummaryDto {
        private Long id;
        private String name;
        private String email;
        private Long assetCount;
        private BigDecimal profitLoss;
    }
    
    @Data
    @Builder
    public static class PortfolioPerformanceData {
        private List<String> labels; // Dates
        private List<BigDecimal> data; // Values
    }
    
    @Data
    @Builder
    public static class TopAssetDto {
        private String name;
        private String category;
        private BigDecimal currentValue;
        private BigDecimal returns; // Percentage
    }
}
