package com.example.CapitalX.beans;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_uploads")
public class PortfolioUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uploadId;


    public Long getUploadId() {
        return uploadId;
    }

    public Long getId() {
        return this.uploadId;
    }


    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(nullable = false)
    private String periodLabel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    private String fileName;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }

    public enum PeriodType {
        QUARTERLY, ANNUAL, CUSTOM
    }
}
