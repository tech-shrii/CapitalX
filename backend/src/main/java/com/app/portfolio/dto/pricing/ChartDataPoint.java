package com.app.portfolio.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {
    private String time;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
}
