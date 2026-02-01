package com.example.CapitalX.service;

import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.dto.PortfolioSnapshotDTO;
import com.example.CapitalX.beans.PortfolioUpload.PeriodType;

import java.util.List;

public interface PortfolioSnapshotService {
    PortfolioSnapshotDTO getLatestSnapshot(Long customerId);
    PortfolioSnapshotDTO getSnapshotByUploadId(Long uploadId);
    List<PortfolioSnapshotDTO> getSnapshotsByCustomer(Long customerId);
    List<PortfolioSnapshotDTO> getSnapshotsByPeriod(Long customerId, PeriodType periodType);
    List<PortfolioSnapshotDTO> getSnapshotsByYear(Long customerId, Integer year);
}
