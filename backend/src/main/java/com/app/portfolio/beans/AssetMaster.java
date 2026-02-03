package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "assets_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long id;

    @Column(name = "asset_code", unique = true, nullable = false, length = 50)
    private String assetCode;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "exchange_or_market", length = 100)
    private String exchangeOrMarket;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum AssetType {
        STOCK, CRYPTO, COMMODITY, ETF, OTHER
    }
}
