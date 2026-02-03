package com.app.portfolio.service.asset;

import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;

import java.util.List;
import java.util.Map;

public interface AssetService {

    List<AssetResponse> getAssetsByClientId(Long clientId, Long userId);

    List<AssetResponse> getAllAssets(Long userId);

    AssetResponse getAssetById(Long id, Long userId);

    AssetResponse createAsset(Long clientId, AssetRequest request, Long userId);

    AssetResponse updateAsset(Long id, AssetRequest request, Long userId);

    void deleteAsset(Long id, Long userId);

    PnlResponse calculatePnL(Long clientId, Long userId);

    int importAssets(List<Map<String, Object>> assets, Long userId);
    
    /**
     * Import assets from CSV file for a specific client
     * @param file CSV file containing asset data
     * @param clientId Client ID to import assets for
     * @param userId User ID for authorization
     * @return Number of assets imported
     */
    int importAssetsFromCSV(java.io.InputStream csvInputStream, Long clientId, Long userId) throws java.io.IOException;

    String exportAssetsAsCSV(Long userId);
}
