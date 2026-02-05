package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "asset_prices", indexes = {
    @Index(name = "idx_asset_prices_symbol", columnList = "symbol"),
    @Index(name = "idx_asset_prices_symbol_date", columnList = "symbol,price_date")
})
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
    @JoinColumn(name = "asset_id", nullable = true)
    private Asset asset;

    @Column(nullable = false, length = 50)
    private String symbol;

    @Column(name = "current_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "price_date", nullable = false)
    private Instant priceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceSource source;

    public enum PriceSource {
        YFINANCE, MANUAL, FAKE, OPEN
    }
}
