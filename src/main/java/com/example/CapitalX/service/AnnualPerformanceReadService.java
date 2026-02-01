package com.example.CapitalX.service;

import com.example.CapitalX.dto.AnnualReportDTO;

import java.util.List;

public interface AnnualPerformanceReadService {
    AnnualReportDTO getAnnualPerformance(Long customerId, Integer financialYear);
    List<AnnualReportDTO> getAllAnnualPerformance(Long customerId);
    List<AnnualReportDTO> getAnnualPerformanceByYear(Integer financialYear);
}
