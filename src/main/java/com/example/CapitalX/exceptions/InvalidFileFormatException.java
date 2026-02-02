package com.example.CapitalX.exceptions;

/**
 * Thrown when the uploaded file name does not match the expected format:
 * customerCode_customerName_period.csv
 */
public class InvalidFileFormatException extends RuntimeException {
    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
