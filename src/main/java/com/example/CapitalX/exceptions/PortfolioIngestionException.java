package com.example.CapitalX.exceptions;

/**
 * Thrown when portfolio ingestion fails for reasons other than file format or CSV validity
 */
public class PortfolioIngestionException extends RuntimeException {
    public PortfolioIngestionException(String message) {
        super(message);
    }

    public PortfolioIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
