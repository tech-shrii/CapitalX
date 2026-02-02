package com.example.CapitalX.util;

import com.example.CapitalX.beans.Asset;
import com.example.CapitalX.dto.PortfolioHoldingCSVDTO;
import com.example.CapitalX.exceptions.InvalidCSVException;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility to parse CSV/Excel files and extract portfolio holdings
 */
public class CSVParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
        "asset_code", "asset_name", "asset_type", "exchange_or_market",
        "quantity", "buy_price", "current_price",
        "invested_value", "current_value", "profit_loss",
        "investment_start_date"
    );

    /**
     * Parse CSV file content
     * @param inputStream CSV file stream
     * @return List of PortfolioHoldingCSVDTO objects
     * @throws InvalidCSVException if CSV is invalid or missing required columns
     */
    public static List<PortfolioHoldingCSVDTO> parseCSV(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                 .withSkipLines(0)
                 .build()) {

            List<PortfolioHoldingCSVDTO> holdings = new ArrayList<>();
            String[] nextLine;
            String[] headers = null;
            Map<String, Integer> columnIndexMap = new HashMap<>();
            int rowNumber = 0;

            try {
                while ((nextLine = csvReader.readNext()) != null) {
                    rowNumber++;

                    if (rowNumber == 1) {
                        // Parse header row
                        headers = nextLine;
                        columnIndexMap = buildColumnIndexMap(headers);
                        continue;
                    }

                    // Skip empty rows
                    if (isEmptyRow(nextLine)) {
                        continue;
                    }

                    try {
                        PortfolioHoldingCSVDTO holding = parseRow(nextLine, columnIndexMap, headers, rowNumber);
                        holdings.add(holding);
                    } catch (Exception e) {
                        throw new InvalidCSVException(
                            "Error parsing row " + rowNumber + ": " + e.getMessage(),
                            e
                        );
                    }
                }
            } catch (CsvValidationException e) {
                throw new InvalidCSVException("CSV validation error at row " + rowNumber + ": " + e.getMessage(), e);
            }

            if (holdings.isEmpty()) {
                throw new InvalidCSVException("CSV file contains no valid holdings data");
            }

            return holdings;

        } catch (IOException e) {
            throw new InvalidCSVException("Failed to read CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse Excel (XLSX or XLS) file content
     * @param inputStream Excel file stream
     * @param filename filename to detect format (.xls vs .xlsx)
     * @return List of PortfolioHoldingCSVDTO objects
     * @throws InvalidCSVException if Excel is invalid or missing required columns
     */
    public static List<PortfolioHoldingCSVDTO> parseExcel(InputStream inputStream, String filename) {
        try {
            Workbook workbook;
            if (filename.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (filename.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new InvalidCSVException("Unsupported Excel format. Use .xlsx or .xls");
            }

            Sheet sheet = workbook.getSheetAt(0);
            List<PortfolioHoldingCSVDTO> holdings = new ArrayList<>();
            Map<String, Integer> columnIndexMap = new HashMap<>();
            int rowNumber = 0;

            for (Row row : sheet) {
                rowNumber++;

                if (rowNumber == 1) {
                    // Parse header row
                    columnIndexMap = buildColumnIndexMapFromExcelRow(row);
                    continue;
                }

                // Skip empty rows
                if (isExcelRowEmpty(row)) {
                    continue;
                }

                try {
                    PortfolioHoldingCSVDTO holding = parseExcelRow(row, columnIndexMap, rowNumber);
                    holdings.add(holding);
                } catch (Exception e) {
                    throw new InvalidCSVException(
                        "Error parsing row " + rowNumber + ": " + e.getMessage(),
                        e
                    );
                }
            }

            workbook.close();

            if (holdings.isEmpty()) {
                throw new InvalidCSVException("Excel file contains no valid holdings data");
            }

            return holdings;

        } catch (IOException e) {
            throw new InvalidCSVException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Build mapping of column names to indices from CSV header
     */
    private static Map<String, Integer> buildColumnIndexMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        Set<String> providedColumns = new HashSet<>();

        for (int i = 0; i < headers.length; i++) {
            String columnName = headers[i].trim().toLowerCase();
            map.put(columnName, i);
            providedColumns.add(columnName);
        }

        // Validate required columns
        Set<String> missingColumns = new HashSet<>(REQUIRED_COLUMNS);
        missingColumns.removeAll(providedColumns);

        if (!missingColumns.isEmpty()) {
            throw new InvalidCSVException(
                "CSV missing required columns: " + missingColumns
            );
        }

        return map;
    }

    /**
     * Build mapping of column names to indices from Excel row
     */
    private static Map<String, Integer> buildColumnIndexMapFromExcelRow(Row row) {
        Map<String, Integer> map = new HashMap<>();
        Set<String> providedColumns = new HashSet<>();

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String columnName = cell.getStringCellValue().trim().toLowerCase();
                map.put(columnName, i);
                providedColumns.add(columnName);
            }
        }

        // Validate required columns
        Set<String> missingColumns = new HashSet<>(REQUIRED_COLUMNS);
        missingColumns.removeAll(providedColumns);

        if (!missingColumns.isEmpty()) {
            throw new InvalidCSVException(
                "Excel missing required columns: " + missingColumns
            );
        }

        return map;
    }

    /**
     * Parse a CSV row into PortfolioHoldingCSVDTO
     */
    private static PortfolioHoldingCSVDTO parseRow(String[] row, Map<String, Integer> columnIndexMap, String[] headers, int rowNumber) {
        PortfolioHoldingCSVDTO holding = new PortfolioHoldingCSVDTO();

        try {
            // Asset fields
            holding.setAssetCode(getStringValue(row, columnIndexMap, "asset_code"));
            holding.setAssetName(getStringValue(row, columnIndexMap, "asset_name"));
            holding.setAssetType(getAssetType(row, columnIndexMap, "asset_type"));
            holding.setExchangeOrMarket(getStringValue(row, columnIndexMap, "exchange_or_market"));

            // Quantity and prices
            holding.setQuantity(getBigDecimalValue(row, columnIndexMap, "quantity"));
            holding.setBuyPrice(getBigDecimalValue(row, columnIndexMap, "buy_price"));
            holding.setCurrentPrice(getBigDecimalValue(row, columnIndexMap, "current_price"));

            // Value fields
            holding.setInvestedValue(getBigDecimalValue(row, columnIndexMap, "invested_value"));
            holding.setCurrentValue(getBigDecimalValue(row, columnIndexMap, "current_value"));
            holding.setProfitLoss(getBigDecimalValue(row, columnIndexMap, "profit_loss"));

            // Date fields
            holding.setInvestmentStartDate(getLocalDateValue(row, columnIndexMap, "investment_start_date"));
            holding.setInvestmentEndDate(getLocalDateValue(row, columnIndexMap, "investment_end_date"));

            return holding;
        } catch (NumberFormatException e) {
            throw new InvalidCSVException("Invalid number format in row " + rowNumber + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parse an Excel row into PortfolioHoldingCSVDTO
     */
    private static PortfolioHoldingCSVDTO parseExcelRow(Row row, Map<String, Integer> columnIndexMap, int rowNumber) {
        PortfolioHoldingCSVDTO holding = new PortfolioHoldingCSVDTO();

        try {
            // Asset fields
            holding.setAssetCode(getStringValueFromExcel(row, columnIndexMap, "asset_code"));
            holding.setAssetName(getStringValueFromExcel(row, columnIndexMap, "asset_name"));
            holding.setAssetType(getAssetTypeFromExcel(row, columnIndexMap, "asset_type"));
            holding.setExchangeOrMarket(getStringValueFromExcel(row, columnIndexMap, "exchange_or_market"));

            // Quantity and prices
            holding.setQuantity(getBigDecimalValueFromExcel(row, columnIndexMap, "quantity"));
            holding.setBuyPrice(getBigDecimalValueFromExcel(row, columnIndexMap, "buy_price"));
            holding.setCurrentPrice(getBigDecimalValueFromExcel(row, columnIndexMap, "current_price"));

            // Value fields
            holding.setInvestedValue(getBigDecimalValueFromExcel(row, columnIndexMap, "invested_value"));
            holding.setCurrentValue(getBigDecimalValueFromExcel(row, columnIndexMap, "current_value"));
            holding.setProfitLoss(getBigDecimalValueFromExcel(row, columnIndexMap, "profit_loss"));

            // Date fields
            holding.setInvestmentStartDate(getLocalDateValueFromExcel(row, columnIndexMap, "investment_start_date"));
            holding.setInvestmentEndDate(getLocalDateValueFromExcel(row, columnIndexMap, "investment_end_date"));

            return holding;
        } catch (NumberFormatException e) {
            throw new InvalidCSVException("Invalid number format in row " + rowNumber + ": " + e.getMessage(), e);
        }
    }

    private static String getStringValue(String[] row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer index = columnIndexMap.get(columnName);
        if (index == null || index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal getBigDecimalValue(String[] row, Map<String, Integer> columnIndexMap, String columnName) {
        String value = getStringValue(row, columnIndexMap, columnName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return new BigDecimal(value);
    }

    private static Asset.AssetType getAssetType(String[] row, Map<String, Integer> columnIndexMap, String columnName) {
        String value = getStringValue(row, columnIndexMap, columnName);
        if (value == null || value.isEmpty()) {
            throw new InvalidCSVException("Asset type cannot be empty");
        }
        try {
            return Asset.AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCSVException("Invalid asset type: " + value + ". Valid types: STOCK, CRYPTO, COMMODITY, ETF, OTHER");
        }
    }

    private static LocalDate getLocalDateValue(String[] row, Map<String, Integer> columnIndexMap, String columnName) {
        String value = getStringValue(row, columnIndexMap, columnName);
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Try multiple date formats
        String[] dateFormats = {"yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String format : dateFormats) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                // Try next format
            }
        }

        throw new InvalidCSVException("Cannot parse date '" + value + "'. Supported formats: yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy, dd/MM/yyyy");
    }

    private static String getStringValueFromExcel(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer index = columnIndexMap.get(columnName);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        String value = cell.getStringCellValue().trim();
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal getBigDecimalValueFromExcel(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer index = columnIndexMap.get(columnName);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return new BigDecimal(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) {
                return null;
            }
            return new BigDecimal(value);
        }
        return null;
    }

    private static Asset.AssetType getAssetTypeFromExcel(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        String value = getStringValueFromExcel(row, columnIndexMap, columnName);
        if (value == null || value.isEmpty()) {
            throw new InvalidCSVException("Asset type cannot be empty");
        }
        try {
            return Asset.AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCSVException("Invalid asset type: " + value + ". Valid types: STOCK, CRYPTO, COMMODITY, ETF, OTHER");
        }
    }

    private static LocalDate getLocalDateValueFromExcel(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer index = columnIndexMap.get(columnName);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) {
                return null;
            }

            String[] dateFormats = {"yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
            for (String format : dateFormats) {
                try {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern(format));
                } catch (Exception e) {
                    // Try next format
                }
            }

            throw new InvalidCSVException("Cannot parse date '" + value + "'. Supported formats: yyyy-MM-dd, dd-MM-yyyy, MM/dd/yyyy, dd/MM/yyyy");
        }

        return null;
    }

    private static boolean isEmptyRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isExcelRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
