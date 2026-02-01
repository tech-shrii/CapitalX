package com.example.CapitalX.service;

import com.example.CapitalX.beans.AnnualPerformance;
import com.example.CapitalX.dto.AnnualReportDTO;
import com.example.CapitalX.exceptions.AnnualPerformanceNotAvailableException;
import com.example.CapitalX.mapper.AnnualPerformanceMapper;
import com.example.CapitalX.repository.AnnualPerformanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnualPerformanceReadServiceImpl implements AnnualPerformanceReadService {
    private final AnnualPerformanceRepository performanceRepository;

    public AnnualPerformanceReadServiceImpl(AnnualPerformanceRepository performanceRepository) {
        this.performanceRepository = performanceRepository;
    }

    @Override
    public AnnualReportDTO getAnnualPerformance(Long customerId, Integer financialYear) {
        AnnualPerformance performance = performanceRepository.findByCustomerIdAndFinancialYear(customerId, financialYear)
            .orElseThrow(() -> new AnnualPerformanceNotAvailableException(
                "Annual performance not found for customer: " + customerId + ", year: " + financialYear));

        return AnnualPerformanceMapper.convertBeanToDTO(performance);
    }

    @Override
    public List<AnnualReportDTO> getAllAnnualPerformance(Long customerId) {
        List<AnnualPerformance> performances = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customerId);
        return performances.stream()
            .map(AnnualPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AnnualReportDTO> getAnnualPerformanceByYear(Integer financialYear) {
        List<AnnualPerformance> performances = performanceRepository.findAll()
            .stream()
            .filter(p -> p.getFinancialYear().equals(financialYear))
            .collect(Collectors.toList());

        return performances.stream()
            .map(AnnualPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }
}
