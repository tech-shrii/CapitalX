package com.example.CapitalX.exceptions;

public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }

    public InvalidPeriodException(String message, Throwable cause) {
        super(message, cause);
    }
}
