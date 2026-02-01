package com.example.CapitalX.service;

import com.example.CapitalX.dto.AssetPerformanceDTO;
import com.example.CapitalX.beans.Asset.AssetType;

import java.util.List;

public interface PortfolioHoldingReadService {
    List<AssetPerformanceDTO> getHoldingsByUpload(Long uploadId);
    List<AssetPerformanceDTO> getHoldingsByCustomerLatest(Long customerId);
    List<AssetPerformanceDTO> getActiveHoldings(Long customerId);
    List<AssetPerformanceDTO> getExitedHoldings(Long customerId);
    List<AssetPerformanceDTO> getProfitableHoldings(Long customerId);
    List<AssetPerformanceDTO> getLossHoldings(Long customerId);
    List<AssetPerformanceDTO> getHoldingsByAssetType(Long customerId, AssetType assetType);
}
