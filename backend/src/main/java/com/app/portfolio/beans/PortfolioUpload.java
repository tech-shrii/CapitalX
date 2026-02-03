package com.app.portfolio.beans;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "portfolio_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_label", nullable = false, length = 100)
    private String periodLabel;

    @Column(name = "upload_date", updatable = false)
    private Instant uploadDate;

    @Column(name = "file_name")
    private String fileName;

    @PrePersist
    protected void onCreate() {
        uploadDate = Instant.now();
    }

    public enum PeriodType {
        QUARTERLY, ANNUAL, CUSTOM
    }
}
