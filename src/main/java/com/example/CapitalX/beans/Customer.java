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
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    public Long getCustomerId() {
        return customerId;
    }

    // Add this to expose the generic 'id' property used by derived queries
    public Long getId() {
        return this.customerId;
    }

    @Column(nullable = false, unique = true)
    private String customerCode;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
