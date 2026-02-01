package com.example.CapitalX.mapper;

import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.beans.PortfolioSummary;
import com.example.CapitalX.dto.PortfolioSnapshotDTO;

import java.math.BigDecimal;

public class PortfolioMapper {

    public static PortfolioSnapshotDTO convertToSnapshotDTO(PortfolioUpload upload, PortfolioSummary summary) {
        if (upload == null) {
            return null;
        }

        PortfolioSnapshotDTO dto = new PortfolioSnapshotDTO();
        dto.setUploadId(upload.getUploadId());
        dto.setPeriodLabel(upload.getPeriodLabel());
        dto.setPeriodType(upload.getPeriodType().toString());
        dto.setUploadDate(upload.getUploadDate());
        dto.setFileName(upload.getFileName());

        if (summary != null) {
            dto.setTotalInvestedValue(summary.getTotalInvestedValue());
            dto.setTotalCurrentValue(summary.getTotalCurrentValue());
            dto.setTotalProfitLoss(summary.getTotalProfitLoss());
            dto.setNumberOfAssets(summary.getNumberOfAssets());
            dto.setNumberOfProfitableAssets(summary.getNumberOfProfitableAssets());
            dto.setNumberOfLossAssets(summary.getNumberOfLossAssets());
        }

        return dto;
    }
}
