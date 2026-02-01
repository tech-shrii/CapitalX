package com.example.CapitalX.controller;

import com.example.CapitalX.dto.CustomerOverviewDTO;
import com.example.CapitalX.dto.PortfolioSnapshotDTO;
import com.example.CapitalX.dto.AssetPerformanceDTO;
import com.example.CapitalX.dto.AnnualReportDTO;
import com.example.CapitalX.service.CustomerReadService;
import com.example.CapitalX.service.PortfolioSnapshotService;
import com.example.CapitalX.service.PortfolioHoldingReadService;
import com.example.CapitalX.service.AnnualPerformanceReadService;
import com.example.CapitalX.beans.Asset.AssetType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/customer")
public class CustomerPortfolioController {
    private final CustomerReadService customerReadService;
    private final PortfolioSnapshotService snapshotService;
    private final PortfolioHoldingReadService holdingService;
    private final AnnualPerformanceReadService performanceService;

    public CustomerPortfolioController(CustomerReadService customerReadService,
                                      PortfolioSnapshotService snapshotService,
                                      PortfolioHoldingReadService holdingService,
                                      AnnualPerformanceReadService performanceService) {
        this.customerReadService = customerReadService;
        this.snapshotService = snapshotService;
        this.holdingService = holdingService;
        this.performanceService = performanceService;
    }

    /**
     * Get customer profile and metadata
     */
    @GetMapping("/{customerId}/profile")
    public ResponseEntity<CustomerOverviewDTO> getCustomerProfile(@PathVariable Long customerId) {
        CustomerOverviewDTO customer = customerReadService.getCustomerById(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get latest portfolio snapshot for a customer
     * Answer: "What is Client A's portfolio worth today?"
     */
    @GetMapping("/{customerId}/portfolio/latest")
    public ResponseEntity<PortfolioSnapshotDTO> getLatestPortfolioSnapshot(@PathVariable Long customerId) {
        PortfolioSnapshotDTO snapshot = snapshotService.getLatestSnapshot(customerId);
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get all portfolio snapshots for a customer (historical data)
     */
    @GetMapping("/{customerId}/portfolio/history")
    public ResponseEntity<List<PortfolioSnapshotDTO>> getPortfolioHistory(@PathVariable Long customerId) {
        List<PortfolioSnapshotDTO> snapshots = snapshotService.getSnapshotsByCustomer(customerId);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get portfolio snapshot for a specific period
     */
    @GetMapping("/{customerId}/portfolio/snapshot/{uploadId}")
    public ResponseEntity<PortfolioSnapshotDTO> getPortfolioSnapshot(
            @PathVariable Long customerId,
            @PathVariable Long uploadId) {
        PortfolioSnapshotDTO snapshot = snapshotService.getSnapshotByUploadId(uploadId);
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get portfolio snapshots filtered by period type (Quarterly, Annual, Custom)
     */
    @GetMapping("/{customerId}/portfolio/by-period")
    public ResponseEntity<List<PortfolioSnapshotDTO>> getPortfolioByPeriod(
            @PathVariable Long customerId,
            @RequestParam String periodType) {
        List<PortfolioSnapshotDTO> snapshots = snapshotService.getSnapshotsByPeriod(
            customerId,
            com.example.CapitalX.beans.PortfolioUpload.PeriodType.valueOf(periodType)
        );
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get portfolio snapshots by year
     */
    @GetMapping("/{customerId}/portfolio/by-year/{year}")
    public ResponseEntity<List<PortfolioSnapshotDTO>> getPortfolioByYear(
            @PathVariable Long customerId,
            @PathVariable Integer year) {
        List<PortfolioSnapshotDTO> snapshots = snapshotService.getSnapshotsByYear(customerId, year);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get all holdings for customer's latest portfolio
     */
    @GetMapping("/{customerId}/holdings/latest")
    public ResponseEntity<List<AssetPerformanceDTO>> getLatestHoldings(@PathVariable Long customerId) {
        List<AssetPerformanceDTO> holdings = holdingService.getHoldingsByCustomerLatest(customerId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get active holdings (not exited)
     */
    @GetMapping("/{customerId}/holdings/active")
    public ResponseEntity<List<AssetPerformanceDTO>> getActiveHoldings(@PathVariable Long customerId) {
        List<AssetPerformanceDTO> holdings = holdingService.getActiveHoldings(customerId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get exited holdings (sold out)
     */
    @GetMapping("/{customerId}/holdings/exited")
    public ResponseEntity<List<AssetPerformanceDTO>> getExitedHoldings(@PathVariable Long customerId) {
        List<AssetPerformanceDTO> holdings = holdingService.getExitedHoldings(customerId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get profitable holdings
     * Answer: "Which assets of Client A are making money?"
     */
    @GetMapping("/{customerId}/holdings/profitable")
    public ResponseEntity<List<AssetPerformanceDTO>> getProfitableHoldings(@PathVariable Long customerId) {
        List<AssetPerformanceDTO> holdings = holdingService.getProfitableHoldings(customerId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get loss-making holdings
     * Answer: "Which assets of Client A are losing money?"
     */
    @GetMapping("/{customerId}/holdings/losses")
    public ResponseEntity<List<AssetPerformanceDTO>> getLossHoldings(@PathVariable Long customerId) {
        List<AssetPerformanceDTO> holdings = holdingService.getLossHoldings(customerId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get holdings by asset type (STOCK, CRYPTO, COMMODITY, ETF, OTHER)
     */
    @GetMapping("/{customerId}/holdings/by-type/{assetType}")
    public ResponseEntity<List<AssetPerformanceDTO>> getHoldingsByType(
            @PathVariable Long customerId,
            @PathVariable String assetType) {
        List<AssetPerformanceDTO> holdings = holdingService.getHoldingsByAssetType(
            customerId,
            AssetType.valueOf(assetType)
        );
        return ResponseEntity.ok(holdings);
    }

    /**
     * Get annual performance for a customer in a specific financial year
     * Answer: "How did Client A perform in FY-2025?"
     */
    @GetMapping("/{customerId}/annual-performance/{financialYear}")
    public ResponseEntity<AnnualReportDTO> getAnnualPerformance(
            @PathVariable Long customerId,
            @PathVariable Integer financialYear) {
        AnnualReportDTO report = performanceService.getAnnualPerformance(customerId, financialYear);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all annual performance records for a customer
     */
    @GetMapping("/{customerId}/annual-performance/all")
    public ResponseEntity<List<AnnualReportDTO>> getAllAnnualPerformance(@PathVariable Long customerId) {
        List<AnnualReportDTO> reports = performanceService.getAllAnnualPerformance(customerId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get customer's portfolio value and P&L summary
     */
    @GetMapping("/{customerId}/summary")
    public ResponseEntity<PortfolioSnapshotDTO> getPortfolioValueAndPL(@PathVariable Long customerId) {
        PortfolioSnapshotDTO summary = snapshotService.getLatestSnapshot(customerId);
        return ResponseEntity.ok(summary);
    }
}
