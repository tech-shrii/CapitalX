package com.example.CapitalX.beans;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_summary")
public class PortfolioSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @ManyToOne
    @JoinColumn(name = "upload_id", nullable = false)
    private PortfolioUpload upload;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInvestedValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCurrentValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(nullable = false)
    private Integer numberOfAssets;

    @Column(nullable = false)
    private Integer numberOfProfitableAssets;

    @Column(nullable = false)
    private Integer numberOfLossAssets;
}
