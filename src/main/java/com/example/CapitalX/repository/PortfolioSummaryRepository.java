package com.example.CapitalX.repository;

import com.example.CapitalX.beans.PortfolioSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioSummaryRepository extends JpaRepository<PortfolioSummary, Long> {
    Optional<PortfolioSummary> findByUploadId(Long uploadId);
    Optional<PortfolioSummary> findByUploadIdAndCustomerId(Long uploadId, Long customerId);
}
