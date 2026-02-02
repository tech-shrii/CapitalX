package com.example.CapitalX.service;

import com.example.CapitalX.beans.*;
import com.example.CapitalX.dto.PortfolioHoldingCSVDTO;
import com.example.CapitalX.exceptions.*;
import com.example.CapitalX.repository.*;
import com.example.CapitalX.util.CSVParser;
import com.example.CapitalX.util.FilenameParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for orchestrating complete portfolio ingestion from CSV/Excel files
 *
 * Workflow:
 * 1. Parse and validate filename → extract customer code, name, period metadata
 * 2. Resolve or create customer
 * 3. Create portfolio_uploads record
 * 4. Parse CSV content → list of holding DTOs
 * 5. For each holding:
 *    - Resolve or create asset
 *    - Insert portfolio_holding
 * 6. Generate portfolio_summary
 * 7. Trigger annual performance if applicable
 * 8. Return ingestion result
 *
 * All operations are wrapped in a transaction for atomicity.
 */
@Service
public class PortfolioIngestionServiceImpl implements PortfolioIngestionService {

    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final PortfolioUploadRepository uploadRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioSummaryRepository summaryRepository;
    private final AnnualPerformanceRepository annualPerformanceRepository;

    public PortfolioIngestionServiceImpl(
            CustomerRepository customerRepository,
            AssetRepository assetRepository,
            PortfolioUploadRepository uploadRepository,
            PortfolioHoldingRepository holdingRepository,
            PortfolioSummaryRepository summaryRepository,
            AnnualPerformanceRepository annualPerformanceRepository) {
        this.customerRepository = customerRepository;
        this.assetRepository = assetRepository;
        this.uploadRepository = uploadRepository;
        this.holdingRepository = holdingRepository;
        this.summaryRepository = summaryRepository;
        this.annualPerformanceRepository = annualPerformanceRepository;
    }

    @Override
    @Transactional
    public PortfolioIngestionResult ingestPortfolio(MultipartFile file) {
        try {
            // Step 1: Validate and parse filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new InvalidFileFormatException("Uploaded file has no name");
            }

            FilenameParser.FileMetadata metadata = FilenameParser.parseFilename(originalFilename);

            // Step 2: Resolve or create customer
            Customer customer = resolveOrCreateCustomer(metadata.getCustomerCode(), metadata.getCustomerName());

            // Step 3: Create portfolio upload record
            PortfolioUpload upload = createPortfolioUpload(customer, metadata, originalFilename);

            // Step 4: Parse CSV/Excel content
            List<PortfolioHoldingCSVDTO> holdings = parseFileContent(file, originalFilename);

            // Step 5: Process holdings
            for (PortfolioHoldingCSVDTO holdingDTO : holdings) {
                processHolding(upload, customer, holdingDTO);
            }

            // Step 6: Generate portfolio summary
            generatePortfolioSummary(upload, customer, holdings);

            // Step 7: Update annual performance if applicable
            if (metadata.getPeriodType() == PortfolioUpload.PeriodType.ANNUAL) {
                updateAnnualPerformance(customer, upload, holdings);
            }

            // Return success result
            return new PortfolioIngestionResult(
                    upload.getUploadId(),
                    customer.getCustomerId(),
                    customer.getCustomerCode(),
                    customer.getCustomerName(),
                    metadata.getPeriodLabel(),
                    holdings.size(),
                    "Portfolio ingestion successful",
                    true
            );

        } catch (InvalidFileFormatException | InvalidCSVException | AssetResolutionException |
                 PortfolioIngestionException e) {
            // Re-throw known exceptions
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions
            throw new PortfolioIngestionException(
                    "Unexpected error during portfolio ingestion: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Resolve customer by code or create if doesn't exist
     * If customer exists but name differs, update name
     */
    private Customer resolveOrCreateCustomer(String customerCode, String customerName) {
        Optional<Customer> existingCustomer = customerRepository.findByCustomerCode(customerCode);

        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            // Update name if it differs (ID remains stable)
            if (!customer.getCustomerName().equals(customerName)) {
                customer.setCustomerName(customerName);
                customerRepository.save(customer);
            }
            return customer;
        }

        // Create new customer
        Customer newCustomer = new Customer();
        newCustomer.setCustomerCode(customerCode);
        newCustomer.setCustomerName(customerName);
        newCustomer.setCreatedAt(LocalDateTime.now());
        return customerRepository.save(newCustomer);
    }

    /**
     * Create portfolio upload record
     */
    private PortfolioUpload createPortfolioUpload(
            Customer customer,
            FilenameParser.FileMetadata metadata,
            String filename) {
        PortfolioUpload upload = new PortfolioUpload();
        upload.setCustomer(customer);
        upload.setPeriodType(metadata.getPeriodType());
        upload.setPeriodLabel(metadata.getPeriodLabel());
        upload.setFileName(filename);
        upload.setUploadDate(LocalDateTime.now());
        return uploadRepository.save(upload);
    }

    /**
     * Parse CSV or Excel file content
     */
    private List<PortfolioHoldingCSVDTO> parseFileContent(MultipartFile file, String filename) {
        try {
            String lowerFilename = filename.toLowerCase();

            if (lowerFilename.endsWith(".csv")) {
                return CSVParser.parseCSV(file.getInputStream());
            } else if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xls")) {
                return CSVParser.parseExcel(file.getInputStream(), filename);
            } else {
                throw new InvalidFileFormatException("Unsupported file format: " + filename);
            }
        } catch (IOException e) {
            throw new InvalidCSVException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    /**
     * Process individual holding:
     * 1. Resolve or create asset
     * 2. Insert portfolio holding
     */
    private void processHolding(PortfolioUpload upload, Customer customer, PortfolioHoldingCSVDTO holdingDTO) {
        // Validate required fields
        if (holdingDTO.getAssetCode() == null || holdingDTO.getAssetCode().isEmpty()) {
            throw new InvalidCSVException("Asset code cannot be empty");
        }
        if (holdingDTO.getAssetName() == null || holdingDTO.getAssetName().isEmpty()) {
            throw new InvalidCSVException("Asset name cannot be empty");
        }
        if (holdingDTO.getAssetType() == null) {
            throw new InvalidCSVException("Asset type cannot be null");
        }
        if (holdingDTO.getQuantity() == null) {
            throw new InvalidCSVException("Quantity cannot be null");
        }
        if (holdingDTO.getInvestmentStartDate() == null) {
            throw new InvalidCSVException("Investment start date cannot be null");
        }

        // Resolve or create asset
        Asset asset = resolveOrCreateAsset(holdingDTO);

        // Insert portfolio holding
        PortfolioHolding holding = new PortfolioHolding();
        holding.setUpload(upload);
        holding.setCustomer(customer);
        holding.setAsset(asset);
        holding.setQuantity(holdingDTO.getQuantity());
        holding.setBuyPrice(holdingDTO.getBuyPrice());
        holding.setCurrentPrice(holdingDTO.getCurrentPrice());
        holding.setInvestedValue(holdingDTO.getInvestedValue());
        holding.setCurrentValue(holdingDTO.getCurrentValue());
        holding.setProfitLoss(holdingDTO.getProfitLoss());
        holding.setInvestmentStartDate(holdingDTO.getInvestmentStartDate());
        holding.setInvestmentEndDate(holdingDTO.getInvestmentEndDate());

        holdingRepository.save(holding);
    }

    /**
     * Resolve asset by (asset_code, asset_type, exchange_or_market)
     * If not found, create new asset
     */
    private Asset resolveOrCreateAsset(PortfolioHoldingCSVDTO holdingDTO) {
        // Check if asset exists by code (code is unique)
        Optional<Asset> existing = assetRepository.findByAssetCode(holdingDTO.getAssetCode());

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new asset
        Asset newAsset = new Asset();
        newAsset.setAssetCode(holdingDTO.getAssetCode());
        newAsset.setAssetName(holdingDTO.getAssetName());
        newAsset.setAssetType(holdingDTO.getAssetType());
        newAsset.setExchangeOrMarket(holdingDTO.getExchangeOrMarket());
        newAsset.setCreatedAt(LocalDateTime.now());

        return assetRepository.save(newAsset);
    }

    /**
     * Generate portfolio summary after all holdings are inserted
     * Aggregates totals and counts for this upload
     */
    private void generatePortfolioSummary(
            PortfolioUpload upload,
            Customer customer,
            List<PortfolioHoldingCSVDTO> holdings) {

        BigDecimal totalInvestedValue = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        int numberOfProfitable = 0;
        int numberOfLoss = 0;

        for (PortfolioHoldingCSVDTO holding : holdings) {
            if (holding.getInvestedValue() != null) {
                totalInvestedValue = totalInvestedValue.add(holding.getInvestedValue());
            }
            if (holding.getCurrentValue() != null) {
                totalCurrentValue = totalCurrentValue.add(holding.getCurrentValue());
            }
            if (holding.getProfitLoss() != null) {
                totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
                if (holding.getProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
                    numberOfProfitable++;
                } else if (holding.getProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
                    numberOfLoss++;
                }
            }
        }

        PortfolioSummary summary = new PortfolioSummary();
        summary.setUpload(upload);
        summary.setCustomer(customer);
        summary.setTotalInvestedValue(totalInvestedValue);
        summary.setTotalCurrentValue(totalCurrentValue);
        summary.setTotalProfitLoss(totalProfitLoss);
        summary.setNumberOfAssets(holdings.size());
        summary.setNumberOfProfitableAssets(numberOfProfitable);
        summary.setNumberOfLossAssets(numberOfLoss);

        summaryRepository.save(summary);
    }

    /**
     * Update annual performance for the customer and financial year
     * This runs when period_type is ANNUAL
     *
     * Computes:
     * - Opening value (from previous year's closing)
     * - Closing value (current value from this upload)
     * - Best and worst performing assets
     */
    private void updateAnnualPerformance(
            Customer customer,
            PortfolioUpload upload,
            List<PortfolioHoldingCSVDTO> holdings) {

        // Extract financial year from period label (assume format like FY2025 or similar)
        Integer financialYear = extractFinancialYear(upload.getPeriodLabel());

        if (financialYear == null) {
            // If cannot extract year, skip annual performance update
            return;
        }

        // Fetch or create annual performance record
        Optional<AnnualPerformance> existing = annualPerformanceRepository
                .findByCustomerIdAndFinancialYear(customer.getCustomerId(), financialYear);

        AnnualPerformance performance;
        if (existing.isPresent()) {
            performance = existing.get();
        } else {
            performance = new AnnualPerformance();
            performance.setCustomer(customer);
            performance.setFinancialYear(financialYear);
            performance.setOpeningValue(BigDecimal.ZERO);
        }

        // Calculate totals
        BigDecimal closingValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalPL = BigDecimal.ZERO;

        for (PortfolioHoldingCSVDTO holding : holdings) {
            if (holding.getCurrentValue() != null) {
                closingValue = closingValue.add(holding.getCurrentValue());
            }
            if (holding.getInvestedValue() != null) {
                totalInvested = totalInvested.add(holding.getInvestedValue());
            }
            if (holding.getProfitLoss() != null) {
                totalPL = totalPL.add(holding.getProfitLoss());
            }
        }

        // Update fields
        performance.setClosingValue(closingValue);
        performance.setTotalInvestedDuringYear(totalInvested);
        performance.setTotalProfitLoss(totalPL);

        // Find best and worst performing assets
        String bestAsset = null;
        String worstAsset = null;
        BigDecimal bestPL = null;
        BigDecimal worstPL = null;

        for (PortfolioHoldingCSVDTO holding : holdings) {
            if (holding.getProfitLoss() == null) {
                continue;
            }

            if (bestPL == null || holding.getProfitLoss().compareTo(bestPL) > 0) {
                bestPL = holding.getProfitLoss();
                bestAsset = holding.getAssetCode();
            }

            if (worstPL == null || holding.getProfitLoss().compareTo(worstPL) < 0) {
                worstPL = holding.getProfitLoss();
                worstAsset = holding.getAssetCode();
            }
        }

        performance.setBestPerformingAsset(bestAsset);
        performance.setWorstPerformingAsset(worstAsset);

        annualPerformanceRepository.save(performance);
    }

    /**
     * Extract financial year from period label
     * Supports: FY2025, FY-2025, 2025, etc.
     */
    private Integer extractFinancialYear(String periodLabel) {
        if (periodLabel == null) {
            return null;
        }

        // Remove common prefixes
        String cleaned = periodLabel.toUpperCase()
                .replace("FY", "")
                .replace("YEAR", "")
                .replace("-", "");

        // Extract 4-digit year
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4})");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
