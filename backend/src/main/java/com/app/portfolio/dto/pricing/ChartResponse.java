package com.app.portfolio.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartResponse {
    private String symbol;
    private String period;
    private String interval;
    private List<ChartDataPoint> data;
}
