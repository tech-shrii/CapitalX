package com.example.CapitalX.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<String> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(PortfolioSnapshotNotFoundException.class)
    public ResponseEntity<String> handlePortfolioSnapshotNotFoundException(PortfolioSnapshotNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(AnnualPerformanceNotAvailableException.class)
    public ResponseEntity<String> handleAnnualPerformanceNotAvailableException(AnnualPerformanceNotAvailableException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<String> handleInvalidPeriodException(InvalidPeriodException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }

    @ExceptionHandler(DataConsistencyException.class)
    public ResponseEntity<String> handleDataConsistencyException(DataConsistencyException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception ex) {
        return ResponseEntity.status(500).body("An unexpected error occurred: " + ex.getMessage());
    }
}
