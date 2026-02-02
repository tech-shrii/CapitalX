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

    // File Ingestion Exceptions
    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<String> handleInvalidFileFormatException(InvalidFileFormatException ex) {
        return ResponseEntity.status(400).body("Invalid file format: " + ex.getMessage());
    }

    @ExceptionHandler(InvalidCSVException.class)
    public ResponseEntity<String> handleInvalidCSVException(InvalidCSVException ex) {
        return ResponseEntity.status(400).body("Invalid CSV content: " + ex.getMessage());
    }

    @ExceptionHandler(AssetResolutionException.class)
    public ResponseEntity<String> handleAssetResolutionException(AssetResolutionException ex) {
        return ResponseEntity.status(400).body("Asset resolution failed: " + ex.getMessage());
    }

    @ExceptionHandler(PortfolioIngestionException.class)
    public ResponseEntity<String> handlePortfolioIngestionException(PortfolioIngestionException ex) {
        return ResponseEntity.status(400).body("Portfolio ingestion failed: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception ex) {
        return ResponseEntity.status(500).body("An unexpected error occurred: " + ex.getMessage());
    }
}

