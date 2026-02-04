package com.app.portfolio.service.asset;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.AssetMapper;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.service.pricing.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AssetMapper assetMapper;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByClientId(Long clientId, Long userId) {
        if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }
        return assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(clientId)
                .stream()
                .map(asset -> {
                    BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
                    BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
                    return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAllAssets(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        List<Client> clients = user.getClients();
        return clients.stream()
                .flatMap(client -> assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(client.getId()).stream())
                .map(asset -> {
                    BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
                    BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
                    return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
        return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
    }

    @Override
    @Transactional
    public AssetResponse createAsset(Long clientId, AssetRequest request, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }

        Asset asset = assetMapper.toEntity(request, null);
        asset.setClient(client);
        asset = assetRepository.save(asset);
        return assetMapper.toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(Long id, AssetRequest request, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        asset = assetMapper.toEntity(request, asset);
        asset = assetRepository.save(asset);
        BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
        return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id, Long userId) {
        if (!assetRepository.existsByIdAndClientUserId(id, userId)) {
            throw new ResourceNotFoundException("Asset", id);
        }
        assetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PnlResponse calculatePnL(Long clientId, Long userId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", clientId);
        }

        List<Asset> assets = assetRepository.findByClientIdOrderByPurchaseDateTimeDesc(clientId);
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;

        List<AssetResponse> assetResponses = assets.stream()
                .map(asset -> {
                    BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
                    BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(asset, currentPrice);
                    return assetMapper.toResponse(asset, currentPrice, profitLoss, profitLossPercent);
                })
                .collect(Collectors.toList());

        for (Asset asset : assets) {
            BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
            BigDecimal currentPrice = asset.isSold() ? asset.getSellingRate() : pricingService.getCurrentPrice(asset.getId());
            BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
            totalInvested = totalInvested.add(invested);
            totalCurrentValue = totalCurrentValue.add(currentValue);
        }

        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalProfitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return PnlResponse.builder()
                .clientId(clientId)
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .assets(assetResponses)
                .build();
    }

    private BigDecimal calculateProfitLoss(Asset asset, BigDecimal currentPrice) {
        if (currentPrice == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
        BigDecimal currentValue = currentPrice.multiply(asset.getQuantity());
        return currentValue.subtract(invested);
    }

    private BigDecimal calculateProfitLossPercent(Asset asset, BigDecimal currentPrice) {
        if (currentPrice == null || asset.getBuyingRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal profitLoss = calculateProfitLoss(asset, currentPrice);
        BigDecimal invested = asset.getBuyingRate().multiply(asset.getQuantity());
        return invested.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public int importAssets(List<Map<String, Object>> assets, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Get or create default client for user
        Client defaultClient = user.getClients().stream()
                .findFirst()
                .orElseGet(() -> {
                    Client newClient = Client.builder()
                            .name("Default Client")
                            .email("default@portfolio.com")
                            .user(user)
                            .createdAt(Instant.now())
                            .build();
                    return clientRepository.save(newClient);
                });

        int importedCount = 0;
        for (Map<String, Object> assetData : assets) {
            try {
                String categoryStr = assetData.getOrDefault("assetType", "STOCK").toString().toUpperCase();
                Asset.AssetCategory category = Asset.AssetCategory.valueOf(categoryStr);
                
                Instant purchaseDateTime = parseInstant(assetData.get("purchaseDateTime"));
                Instant sellingDateTime = parseInstant(assetData.get("sellingDateTime"));
                
                String currency = assetData.containsKey("currency")
                        ? assetData.get("currency").toString().toUpperCase().substring(0, Math.min(3, assetData.get("currency").toString().length()))
                        : "USD";
                
                BigDecimal sellingRate = assetData.get("sellingRate") != null ? new BigDecimal(assetData.get("sellingRate").toString()) : null;
                boolean sold = Boolean.parseBoolean(assetData.getOrDefault("sold", "false").toString());

                Asset asset = Asset.builder()
                        .symbol(assetData.get("symbol").toString().toUpperCase())
                        .quantity(new BigDecimal(assetData.getOrDefault("quantity", "0").toString()))
                        .buyingRate(new BigDecimal(assetData.getOrDefault("buyingRate", "0").toString()))
                        .category(category)
                        .name(assetData.getOrDefault("name", assetData.get("symbol")).toString())
                        .purchaseDateTime(purchaseDateTime)
                        .currency(currency)
                        .client(defaultClient)
                        .sellingRate(sellingRate)
                        .sellingDateTime(sellingDateTime)
                        .sold(sold)
                        .createdAt(Instant.now())
                        .build();
                assetRepository.save(asset);
                importedCount++;
            } catch (Exception e) {
                // Skip invalid assets
                continue;
            }
        }

        return importedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAssetsAsCSV(Long userId) {
        List<AssetResponse> assets = getAllAssets(userId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("symbol,quantity,buyingRate,assetType,purchaseDateTime,currency,sellingRate,sellingDateTime,sold\n");

        for (AssetResponse asset : assets) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    asset.getSymbol(),
                    asset.getQuantity(),
                    asset.getBuyingRate(),
                    asset.getCategory(),
                    asset.getPurchaseDateTime(),
                    asset.getCurrency() != null ? asset.getCurrency() : "USD",
                    asset.getSellingRate(),
                    asset.getSellingDateTime(),
                    asset.isSold()
            ));
        }

        return csv.toString();
    }

    @Override
    @Transactional
    public int importAssetsFromCSV(InputStream csvInputStream, Long clientId, Long userId) throws IOException {
        log.debug("Importing CSV assets for clientId: {}, userId: {}", clientId, userId);
        
        try {
            // Verify client belongs to user
            if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
                log.warn("Client {} does not belong to user {}", clientId, userId);
                throw new ResourceNotFoundException("Client", clientId);
            }

            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
            log.debug("Client found: {}", client.getName());

            // Parse CSV file
            List<Map<String, Object>> assets = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
                String line = reader.readLine();
                if (line == null) {
                    log.warn("CSV file is empty");
                    return 0;
                }

                // Parse header using the same logic as data rows for consistency
                String[] headers = parseCSVLine(line);
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = headers[i].trim().toLowerCase();
                }
                log.debug("CSV headers: {}", String.join(", ", headers));

                // ... header validation ...

                // Parse data rows
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        String[] values = parseCSVLine(line);
                        if (values.length < headers.length) {
                            log.warn("Line {} has fewer values than headers, skipping", lineNumber);
                            continue;
                        }

                        Map<String, Object> assetData = new HashMap<>();
                        for (int i = 0; i < headers.length && i < values.length; i++) {
                            String value = values[i].trim();
                            if (!value.isEmpty()) {
                                assetData.put(headers[i], value);
                            }
                        }
                        
                        assets.add(assetData);

                    } catch (Exception e) {
                        log.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                        continue;
                    }
                }
            }

            log.debug("Parsed {} assets from CSV", assets.size());

            // Import assets using existing logic
            int importedCount = 0;
            for (Map<String, Object> assetData : assets) {
                try {
                    String categoryStr = assetData.getOrDefault("category", "STOCK").toString().toUpperCase();
                    Asset.AssetCategory category;
                    try {
                        category = Asset.AssetCategory.valueOf(categoryStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid category '{}', defaulting to STOCK", categoryStr);
                        category = Asset.AssetCategory.STOCK;
                    }
                    
                    // Handle variations in purchase date column name ('purchasedatetime' or 'purchasedate')
                    Object purchaseDateObj = assetData.get("purchasedatetime");
                    if (purchaseDateObj == null) {
                        purchaseDateObj = assetData.get("purchasedate");
                    }
                                        Instant purchaseDateTime = parseInstant(purchaseDateObj);
                                        Object sellingDateObj = assetData.get("sellingdatetime");
                                        if (sellingDateObj == null) {
                                            sellingDateObj = assetData.get("sellingdate");
                                        }
                                        Instant sellingDateTime = parseInstant(sellingDateObj);
                    
                                        String currency = assetData.containsKey("currency")
                                            ? assetData.get("currency").toString().toUpperCase().substring(0, Math.min(3, assetData.get("currency").toString().length()))
                                            : "USD";
                                        
                                        BigDecimal quantity = new BigDecimal(assetData.getOrDefault("quantity", "0").toString());
                                        BigDecimal buyingRate = new BigDecimal(assetData.getOrDefault("buyingrate", "0").toString());
                                        Object sellingRateObj = assetData.get("sellingrate");
                                        BigDecimal sellingRate = sellingRateObj != null && !sellingRateObj.toString().isEmpty()
                                                ? new BigDecimal(sellingRateObj.toString())
                                                : null;
                                        boolean sold = Boolean.parseBoolean(assetData.getOrDefault("sold", "false").toString());
                    
                                        
                                        Asset asset = Asset.builder()
                                                .symbol(assetData.get("symbol").toString().toUpperCase())
                                                .quantity(quantity)
                                                .buyingRate(buyingRate)
                                                .category(category)
                                                .name(assetData.getOrDefault("name", assetData.get("symbol")).toString())
                                                .purchaseDateTime(purchaseDateTime)
                                                .currency(currency)
                                                .client(client)
                                                .sellingRate(sellingRate)
                                                .sellingDateTime(sellingDateTime)                            .sold(sold)
                            .createdAt(Instant.now())
                            .build();
                    assetRepository.save(asset);
                    importedCount++;
                    log.debug("Imported asset: {}", asset.getSymbol());
                } catch (Exception e) {
                    log.error("Error importing asset from CSV data: {}", assetData, e);
                    // Re-throw as a runtime exception to ensure the transaction is rolled back
                    throw new com.app.portfolio.exceptions.BadRequestException(
                            "Failed to import asset from CSV row. Data: " + assetData.toString() + ". Error: " + e.getMessage());
                }
            }

            log.info("Successfully imported {}/{} assets for clientId: {}", importedCount, assets.size(), clientId);
            return importedCount;
        } catch (Exception e) {
            log.error("Error importing CSV for clientId: {}, userId: {}", clientId, userId, e);
            throw e;
        }
    }
    
    private Instant parseInstant(Object obj) {
        if (obj == null) return null;
        String dateStr = obj.toString();
        try {
            return Instant.parse(dateStr);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Parse a CSV line handling quoted values
     */
    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}
