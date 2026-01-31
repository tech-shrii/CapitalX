package com.example.CapitalX.beans;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_holdings")
public class PortfolioHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holdingId;

    @ManyToOne
    @JoinColumn(name = "upload_id", nullable = false)
    private PortfolioUpload upload;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal buyPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal investedValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal profitLoss;

    @Column(nullable = false)
    private LocalDate investmentStartDate;

    private LocalDate investmentEndDate;
}
