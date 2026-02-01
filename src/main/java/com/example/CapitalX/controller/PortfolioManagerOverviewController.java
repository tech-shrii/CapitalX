package com.example.CapitalX.controller;

import com.example.CapitalX.dto.PortfolioAnalyticsDTO;
import com.example.CapitalX.dto.CustomerPerformanceDTO;
import com.example.CapitalX.dto.AssetExposureDTO;
import com.example.CapitalX.service.PortfolioAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/manager")
public class PortfolioManagerOverviewController {
    private final PortfolioAnalyticsService analyticsService;

    public PortfolioManagerOverviewController(PortfolioAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get portfolio manager's bird's-eye view of entire book
     * Answer: "How is my entire book performing?"
     */
    @GetMapping("/overview")
    public ResponseEntity<PortfolioAnalyticsDTO> getPortfolioManagerOverview() {
        PortfolioAnalyticsDTO overview = analyticsService.getPortfolioManagerOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Get manager overview filtered by year
     */
    @GetMapping("/overview/by-year/{year}")
    public ResponseEntity<PortfolioAnalyticsDTO> getManagerOverviewByYear(@PathVariable Integer year) {
        PortfolioAnalyticsDTO overview = analyticsService.getManagerOverviewByYear(year);
        return ResponseEntity.ok(overview);
    }

    /**
     * Get top performing customers
     */
    @GetMapping("/top-customers")
    public ResponseEntity<List<CustomerPerformanceDTO>> getTopPerformingCustomers(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<CustomerPerformanceDTO> customers = analyticsService.getTopPerformingCustomers(limit);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get bottom performing customers (dragging returns)
     * Answer: "Which clients are dragging returns?"
     */
    @GetMapping("/bottom-customers")
    public ResponseEntity<List<CustomerPerformanceDTO>> getBottomPerformingCustomers(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<CustomerPerformanceDTO> customers = analyticsService.getBottomPerformingCustomers(limit);
        return ResponseEntity.ok(customers);
    }

    /**
     * Get top performing assets across all customers
     */
    @GetMapping("/top-assets")
    public ResponseEntity<List<AssetExposureDTO>> getTopAssets(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<AssetExposureDTO> assets = analyticsService.getTopAssets(limit);
        return ResponseEntity.ok(assets);
    }

    /**
     * Get risky/underperforming assets across customers
     * Answer: "Which assets are risky across portfolios?"
     */
    @GetMapping("/risky-assets")
    public ResponseEntity<List<AssetExposureDTO>> getRiskyAssets(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<AssetExposureDTO> assets = analyticsService.getRiskyAssets(limit);
        return ResponseEntity.ok(assets);
    }

    /**
     * Get overall portfolio value across all customers
     */
    @GetMapping("/total-portfolio-value")
    public ResponseEntity<PortfolioAnalyticsDTO> getTotalPortfolioValue() {
        PortfolioAnalyticsDTO analytics = analyticsService.getPortfolioManagerOverview();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get asset exposure across all customers
     * Answer: "Which assets are most exposed across portfolios?"
     */
    @GetMapping("/asset-exposure")
    public ResponseEntity<List<AssetExposureDTO>> getAssetExposure(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<AssetExposureDTO> assets = analyticsService.getTopAssets(limit);
        return ResponseEntity.ok(assets);
    }

    /**
     * Get annual P&L across all customers
     */
    @GetMapping("/annual-pnl")
    public ResponseEntity<PortfolioAnalyticsDTO> getAnnualPNL() {
        PortfolioAnalyticsDTO analytics = analyticsService.getPortfolioManagerOverview();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get customer concentration risk
     */
    @GetMapping("/concentration-analysis")
    public ResponseEntity<List<CustomerPerformanceDTO>> getConcentrationAnalysis() {
        List<CustomerPerformanceDTO> customers = analyticsService.getTopPerformingCustomers(100);
        return ResponseEntity.ok(customers);
    }
}
