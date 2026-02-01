package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "asset_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "current_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "price_date", nullable = false)
    private Instant priceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceSource source;

    public enum PriceSource {
        YFINANCE, MANUAL, FAKE
    }
}
