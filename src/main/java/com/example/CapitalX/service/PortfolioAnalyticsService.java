package com.example.CapitalX.service;

import com.example.CapitalX.dto.PortfolioAnalyticsDTO;
import com.example.CapitalX.dto.CustomerPerformanceDTO;
import com.example.CapitalX.dto.AssetExposureDTO;

import java.util.List;

public interface PortfolioAnalyticsService {
    PortfolioAnalyticsDTO getPortfolioManagerOverview();
    PortfolioAnalyticsDTO getManagerOverviewByYear(Integer year);
    List<CustomerPerformanceDTO> getTopPerformingCustomers(Integer limit);
    List<CustomerPerformanceDTO> getBottomPerformingCustomers(Integer limit);
    List<AssetExposureDTO> getTopAssets(Integer limit);
    List<AssetExposureDTO> getRiskyAssets(Integer limit);
}
