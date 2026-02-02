package com.example.CapitalX.service;

import com.example.CapitalX.dto.PortfolioHoldingCSVDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for portfolio ingestion from CSV/Excel files
 */
public interface PortfolioIngestionService {

    /**
     * Ingest a portfolio from an uploaded CSV/Excel file
     *
     * @param file The uploaded CSV or Excel file
     * @return A summary of the ingestion result
     */
    PortfolioIngestionResult ingestPortfolio(MultipartFile file);
}
