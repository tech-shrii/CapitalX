package com.app.portfolio.controller;

import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.asset.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
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

    @PostMapping("/assets")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody AssetRequest request,
                                                     @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(assetService.createAsset(request, userPrincipal.getId()));
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

    @PostMapping("/clients/{clientId}/assets/import-csv")
    public ResponseEntity<Map<String, Object>> importAssetsFromCSV(
            @PathVariable Long clientId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("POST /api/clients/{}/assets/import-csv - userId: {}, fileSize: {}", 
                clientId, userPrincipal.getId(), file.getSize());
        
        try {
            if (file.isEmpty()) {
                log.warn("CSV file is empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CSV file is empty"));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                log.warn("Invalid file type: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File must be a CSV file"));
            }

            log.debug("Processing CSV file: {}", file.getOriginalFilename());
            int count = assetService.importAssetsFromCSV(file.getInputStream(), clientId, userPrincipal.getId());
            log.info("Successfully imported {} assets for clientId: {}", count, clientId);
            
            return ResponseEntity.ok(Map.of(
                    "count", count,
                    "message", "Assets imported successfully"
            ));
        } catch (IOException e) {
            log.error("IO error importing CSV for clientId: {}", clientId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error reading CSV file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error importing CSV for clientId: {}", clientId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error importing CSV: " + e.getMessage()));
        }
    }
}
