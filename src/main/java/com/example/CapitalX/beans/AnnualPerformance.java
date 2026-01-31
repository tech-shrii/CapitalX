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
@Table(name = "annual_performance")
public class AnnualPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long annualPerfId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer financialYear;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal closingValue;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInvestedDuringYear;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    private String bestPerformingAsset;

    private String worstPerformingAsset;
}
