package com.example.CapitalX.repository;

import com.example.CapitalX.beans.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
    List<PortfolioHolding> findByUploadId(Long uploadId);
    List<PortfolioHolding> findByCustomerId(Long customerId);
    List<PortfolioHolding> findByUploadIdAndCustomerId(Long uploadId, Long customerId);
}
