package com.example.CapitalX.repository;

import com.example.CapitalX.beans.PortfolioUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioUploadRepository extends JpaRepository<PortfolioUpload, Long> {
    List<PortfolioUpload> findByCustomerId(Long customerId);
    List<PortfolioUpload> findByCustomerIdOrderByUploadDateDesc(Long customerId);
}
