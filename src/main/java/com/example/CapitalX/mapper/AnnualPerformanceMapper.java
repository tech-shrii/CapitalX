package com.example.CapitalX.mapper;

import com.example.CapitalX.beans.AnnualPerformance;
import com.example.CapitalX.dto.AnnualReportDTO;

import java.math.BigDecimal;

public class AnnualPerformanceMapper {

    public static AnnualReportDTO convertBeanToDTO(AnnualPerformance performance) {
        if (performance == null) {
            return null;
        }

        AnnualReportDTO dto = new AnnualReportDTO();
        dto.setAnnualPerfId(performance.getAnnualPerfId());
        dto.setFinancialYear(performance.getFinancialYear());
        dto.setOpeningValue(performance.getOpeningValue());
        dto.setClosingValue(performance.getClosingValue());
        dto.setTotalInvestedDuringYear(performance.getTotalInvestedDuringYear());
        dto.setTotalProfitLoss(performance.getTotalProfitLoss());
        dto.setBestPerformingAsset(performance.getBestPerformingAsset());
        dto.setWorstPerformingAsset(performance.getWorstPerformingAsset());

        // Calculate return percentage
        if (performance.getOpeningValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal returnPercentage = performance.getTotalProfitLoss()
                .divide(performance.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            dto.setReturnPercentage(returnPercentage);
        }

        return dto;
    }
}
