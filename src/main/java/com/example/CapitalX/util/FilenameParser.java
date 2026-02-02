package com.example.CapitalX.util;

import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.exceptions.InvalidFileFormatException;
import lombok.Getter;

/**
 * Parses and validates filename in format: customerCode_customerName_period.csv
 * Example: CUST001_RaviKumar_FY2025.csv
 */
public class FilenameParser {

    @Getter
    public static class FileMetadata {
        private final String customerCode;
        private final String customerName;
        private final String periodLabel;
        private final PortfolioUpload.PeriodType periodType;

        public FileMetadata(String customerCode, String customerName, String periodLabel, PortfolioUpload.PeriodType periodType) {
            this.customerCode = customerCode;
            this.customerName = customerName;
            this.periodLabel = periodLabel;
            this.periodType = periodType;
        }
    }

    /**
     * Parse filename and extract metadata
     * @param filename e.g., CUST001_RaviKumar_FY2025.csv
     * @return FileMetadata containing customerCode, customerName, period, and periodType
     * @throws InvalidFileFormatException if filename doesn't match pattern
     */
    public static FileMetadata parseFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new InvalidFileFormatException("Filename cannot be null or empty");
        }

        // Remove extension
        String nameWithoutExt;
        if (filename.toLowerCase().endsWith(".csv")) {
            nameWithoutExt = filename.substring(0, filename.length() - 4);
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            nameWithoutExt = filename.substring(0, filename.length() - 5);
        } else if (filename.toLowerCase().endsWith(".xls")) {
            nameWithoutExt = filename.substring(0, filename.length() - 4);
        } else {
            throw new InvalidFileFormatException(
                "Invalid file extension. Expected .csv, .xlsx, or .xls. Got: " + filename
            );
        }

        // Split by underscore
        String[] parts = nameWithoutExt.split("_");
        if (parts.length != 3) {
            throw new InvalidFileFormatException(
                "Invalid filename format. Expected format: customerCode_customerName_period.csv. Got: " + filename
            );
        }

        String customerCode = parts[0].trim();
        String customerName = parts[1].trim();
        String periodLabel = parts[2].trim();

        // Validate customer code
        if (customerCode.isEmpty()) {
            throw new InvalidFileFormatException("Customer code cannot be empty");
        }

        // Validate customer name
        if (customerName.isEmpty()) {
            throw new InvalidFileFormatException("Customer name cannot be empty");
        }

        // Validate period label
        if (periodLabel.isEmpty()) {
            throw new InvalidFileFormatException("Period label cannot be empty");
        }

        // Derive period type from label
        PortfolioUpload.PeriodType periodType = derivePeriodType(periodLabel);

        return new FileMetadata(customerCode, customerName, periodLabel, periodType);
    }

    /**
     * Derive PeriodType from period label
     * @param periodLabel e.g., FY2025, Q1-2026, CUSTOM
     * @return PeriodType.ANNUAL, PeriodType.QUARTERLY, or PeriodType.CUSTOM
     */
    private static PortfolioUpload.PeriodType derivePeriodType(String periodLabel) {
        String upper = periodLabel.toUpperCase();

        // Check for Annual patterns: FY, ANNUAL, Year
        if (upper.startsWith("FY") || upper.contains("ANNUAL") || upper.matches(".*\\d{4}$")) {
            return PortfolioUpload.PeriodType.ANNUAL;
        }

        // Check for Quarterly patterns: Q1, Q2, Q3, Q4
        if (upper.matches("Q[1-4].*") || upper.contains("QUARTER")) {
            return PortfolioUpload.PeriodType.QUARTERLY;
        }

        // Default to CUSTOM
        return PortfolioUpload.PeriodType.CUSTOM;
    }
}
