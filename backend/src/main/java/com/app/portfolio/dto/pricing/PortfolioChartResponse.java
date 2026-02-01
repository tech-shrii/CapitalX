package com.app.portfolio.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioChartResponse {
    private Map<String, Double> portfolio;
    private String period;
    private String interval;
    private List<PortfolioChartDataPoint> data;
}
