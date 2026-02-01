package com.app.portfolio.dto.pricing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceResponse {
    private String symbol;
    private Double price;
    private String timestamp;
    private String currency;
}
