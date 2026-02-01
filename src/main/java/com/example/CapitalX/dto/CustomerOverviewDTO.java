package com.example.CapitalX.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOverviewDTO {
    private Long customerId;
    private String customerCode;
    private String customerName;
    private LocalDateTime createdAt;
}
