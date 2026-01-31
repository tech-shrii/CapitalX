package com.example.CapitalX.repository;

import com.example.CapitalX.beans.AnnualPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnualPerformanceRepository extends JpaRepository<AnnualPerformance, Long> {
    Optional<AnnualPerformance> findByCustomerIdAndFinancialYear(Long customerId, Integer financialYear);
    List<AnnualPerformance> findByCustomerId(Long customerId);
    List<AnnualPerformance> findByCustomerIdOrderByFinancialYearDesc(Long customerId);
}
