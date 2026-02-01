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
        return ResponseEntity.ok().body(java.util.Map.of("message", "Asset deleted successfully"));
    }

    @GetMapping("/clients/{clientId}/pnl")
    public ResponseEntity<PnlResponse> calculatePnL(@PathVariable Long clientId,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.calculatePnL(clientId, userPrincipal.getId()));
    }
}
