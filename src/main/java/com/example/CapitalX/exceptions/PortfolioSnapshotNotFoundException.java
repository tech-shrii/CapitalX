package com.example.CapitalX.exceptions;

public class PortfolioSnapshotNotFoundException extends RuntimeException {
    public PortfolioSnapshotNotFoundException(String message) {
        super(message);
    }
}
