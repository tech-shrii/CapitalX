package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_holdings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id")
    private PortfolioUpload portfolioUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 50)
    private String ticker;

    @Column(length = 10)
    private String currency;

    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "buy_price", precision = 18, scale = 6)
    private BigDecimal buyPrice;

    @Column(name = "current_price", precision = 18, scale = 6)
    private BigDecimal currentPrice;

    @Column(name = "invested_value", precision = 18, scale = 2)
    private BigDecimal investedValue;

    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "profit_loss", precision = 18, scale = 2)
    private BigDecimal profitLoss;

    @Column(name = "investment_start_date")
    private LocalDate investmentStartDate;

    @Column(name = "investment_end_date")
    private LocalDate investmentEndDate;
}
