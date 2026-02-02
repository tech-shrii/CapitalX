package com.example.CapitalX.exceptions;

/**
 * Thrown when CSV content is invalid or missing required columns
 */
public class InvalidCSVException extends RuntimeException {
    public InvalidCSVException(String message) {
        super(message);
    }

    public InvalidCSVException(String message, Throwable cause) {
        super(message, cause);
    }
}
