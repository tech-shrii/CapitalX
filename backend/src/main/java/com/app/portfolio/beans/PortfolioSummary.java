package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolio_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", unique = true)
    private PortfolioUpload portfolioUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "total_invested_value", precision = 18, scale = 2)
    private BigDecimal totalInvestedValue;

    @Column(name = "total_current_value", precision = 18, scale = 2)
    private BigDecimal totalCurrentValue;

    @Column(name = "total_profit_loss", precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(name = "number_of_assets")
    private Integer numberOfAssets;

    @Column(name = "number_of_profitable_assets")
    private Integer numberOfProfitableAssets;

    @Column(name = "number_of_loss_assets")
    private Integer numberOfLossAssets;
}
