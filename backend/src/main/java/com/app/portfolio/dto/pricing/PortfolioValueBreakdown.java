package com.app.portfolio.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioValueBreakdown {
    private Double price;
    private Double quantity;
    private Double value;
    private String error;
}
