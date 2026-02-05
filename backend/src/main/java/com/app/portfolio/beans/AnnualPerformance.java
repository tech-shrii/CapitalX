package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "annual_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // for easy object creation
public class AnnualPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annual_perf_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "financial_year")
    private Integer financialYear;

    @Column(name = "opening_value", precision = 18, scale = 2)
    private BigDecimal openingValue;

    @Column(name = "closing_value", precision = 18, scale = 2)
    private BigDecimal closingValue;

    @Column(name = "total_invested_during_year", precision = 18, scale = 2)
    private BigDecimal totalInvestedDuringYear;

    @Column(name = "total_profit_loss", precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(name = "best_performing_asset")
    private String bestPerformingAsset;

    @Column(name = "worst_performing_asset")
    private String worstPerformingAsset;
}
