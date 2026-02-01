package com.app.portfolio.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioValueResponse {
    private Double totalValue;
    private Map<String, PortfolioValueBreakdown> breakdown;
    private String timestamp;
}
