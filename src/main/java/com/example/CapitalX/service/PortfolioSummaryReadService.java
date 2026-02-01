package com.example.CapitalX.service;

import com.example.CapitalX.dto.PortfolioSnapshotDTO;

import java.util.List;

public interface PortfolioSummaryReadService {
    PortfolioSnapshotDTO getPortfolioSummary(Long uploadId);
    List<PortfolioSnapshotDTO> compareSnapshots(Long customerId);
}
