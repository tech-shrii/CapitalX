package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCategory category;

    @Column(length = 50)
    private String symbol;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal quantity;

    @Column(name = "buying_rate", nullable = false, precision = 20, scale = 4)
    private BigDecimal buyingRate;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priceDate DESC")
    @Builder.Default
    private List<AssetPrice> prices = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum AssetCategory {
        STOCK, MUTUAL_FUND, BOND, CRYPTO, COMMODITY, FOREX
    }
}
