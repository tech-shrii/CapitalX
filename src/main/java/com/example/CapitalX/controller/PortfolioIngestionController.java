package com.example.CapitalX.controller;

import com.example.CapitalX.service.PortfolioIngestionResult;
import com.example.CapitalX.service.PortfolioIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for portfolio file ingestion
 *
 * Endpoint: POST /api/portfolio/ingest
 *
 * Accepts CSV or Excel files with naming convention:
 * customerCode_customerName_period.csv
 *
 * Responsibilities:
 * - Accept multipart file upload
 * - Validate basic file constraints
 * - Forward to ingestion service
 * - Return result
 *
 * All business logic delegated to PortfolioIngestionService
 */
@RestController
@RequestMapping("/api/portfolio/ingest")
public class PortfolioIngestionController {

    private final PortfolioIngestionService ingestionService;

    public PortfolioIngestionController(PortfolioIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Upload and ingest a portfolio from CSV/Excel file
     *
     * @param file CSV or Excel file with format: customerCode_customerName_period.csv
     * @return PortfolioIngestionResult with upload details
     */
    @PostMapping("/upload")
    public ResponseEntity<PortfolioIngestionResult> uploadPortfolio(
            @RequestParam("file") MultipartFile file) {

        // Basic file validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Forward to ingestion service
        PortfolioIngestionResult result = ingestionService.ingestPortfolio(file);

        // Return result with 200 OK
        return ResponseEntity.ok(result);
    }
}
