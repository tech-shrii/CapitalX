package com.app.portfolio.service.asset;

import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;

import java.util.List;

public interface AssetService {

    List<AssetResponse> getAssetsByClientId(Long clientId, Long userId);

    AssetResponse getAssetById(Long id, Long userId);

    AssetResponse createAsset(Long clientId, AssetRequest request, Long userId);

    AssetResponse updateAsset(Long id, AssetRequest request, Long userId);

    void deleteAsset(Long id, Long userId);

    PnlResponse calculatePnL(Long clientId, Long userId);
}
