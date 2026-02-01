package com.app.portfolio.controller;

import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.asset.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/clients/{clientId}/assets")
    public ResponseEntity<List<AssetResponse>> getAssetsByClientId(@PathVariable Long clientId,
                                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.getAssetsByClientId(clientId, userPrincipal.getId()));
    }

    @GetMapping("/assets")
    public ResponseEntity<List<AssetResponse>> getAllAssets(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.getAllAssets(userPrincipal.getId()));
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.getAssetById(id, userPrincipal.getId()));
    }

    @PostMapping("/clients/{clientId}/assets")
    public ResponseEntity<AssetResponse> createAsset(@PathVariable Long clientId,
                                                      @Valid @RequestBody AssetRequest request,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.createAsset(clientId, request, userPrincipal.getId()));
    }

    @PostMapping("/assets/import")
    public ResponseEntity<Map<String, Object>> importAssets(@RequestBody List<Map<String, Object>> assets,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        int count = assetService.importAssets(assets, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("count", count, "message", "Assets imported successfully"));
    }

    @GetMapping("/assets/export")
    public ResponseEntity<String> exportAssets(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String csv = assetService.exportAssetsAsCSV(userPrincipal.getId());
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"portfolio_export.csv\"")
                .body(csv);
    }

    @PutMapping("/assets/{id}")
    public ResponseEntity<AssetResponse> updateAsset(@PathVariable Long id,
                                                      @Valid @RequestBody AssetRequest request,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.updateAsset(id, request, userPrincipal.getId()));
    }

    @DeleteMapping("/assets/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        assetService.deleteAsset(id, userPrincipal.getId());
        return ResponseEntity.ok().body(Map.of("message", "Asset deleted successfully"));
    }

    @GetMapping("/clients/{clientId}/pnl")
    public ResponseEntity<PnlResponse> calculatePnL(@PathVariable Long clientId,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.calculatePnL(clientId, userPrincipal.getId()));
    }
}
