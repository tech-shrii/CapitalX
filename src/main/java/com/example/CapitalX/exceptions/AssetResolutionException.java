package com.example.CapitalX.exceptions;

/**
 * Thrown when an asset cannot be resolved or created during ingestion
 */
public class AssetResolutionException extends RuntimeException {
    public AssetResolutionException(String message) {
        super(message);
    }

    public AssetResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
