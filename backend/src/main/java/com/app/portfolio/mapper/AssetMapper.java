package com.app.portfolio.mapper;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AssetMapper {

    public Asset toEntity(AssetRequest request, Asset existing) {
        if (existing == null) {
            existing = new Asset();
        }
        existing.setName(request.getName());
        existing.setCategory(request.getCategory());
        existing.setSymbol(request.getSymbol() != null ? request.getSymbol() : deriveSymbol(request.getCategory(), request.getName()));
        existing.setQuantity(request.getQuantity());
        existing.setBuyingRate(request.getBuyingRate());
        existing.setPurchaseDateTime(request.getPurchaseDateTime());
        if (request.getCurrency() != null) {
            existing.setCurrency(request.getCurrency());
        }
        existing.setSellingRate(request.getSellingRate());
        existing.setSellingDateTime(request.getSellingDateTime());
        existing.setSold(request.isSold());
        return existing;
    }

    public AssetResponse toResponse(Asset entity, BigDecimal currentPrice, BigDecimal profitLoss, BigDecimal profitLossPercent) {
        return AssetResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClient().getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .symbol(entity.getSymbol())
                .quantity(entity.getQuantity())
                .buyingRate(entity.getBuyingRate())
                .purchaseDateTime(entity.getPurchaseDateTime())
                .currency(entity.getCurrency())
                .currentPrice(currentPrice)
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPercent)
                .sellingRate(entity.getSellingRate())
                .sellingDateTime(entity.getSellingDateTime())
                .sold(entity.isSold())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AssetResponse toResponse(Asset entity) {
        return toResponse(entity, null, null, null);
    }

    private String deriveSymbol(Asset.AssetCategory category, String name) {
        if (name == null || name.isBlank()) return "";
        return switch (category) {
            case STOCK -> name.toUpperCase().replaceAll("\\s+", ".");
            case CRYPTO -> name.toUpperCase();
            default -> name;
        };
    }
}
