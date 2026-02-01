package com.app.portfolio.service.pricing;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.AssetPrice;
import com.app.portfolio.repository.AssetPriceRepository;
import com.app.portfolio.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final AssetRepository assetRepository;
    private final AssetPriceRepository assetPriceRepository;
    private final Random random = new Random();

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Long assetId) {
        return assetPriceRepository.findFirstByAssetIdOrderByPriceDateDesc(assetId)
                .map(assetPrice -> {
                    // Add random variation for real-time simulation (±0.5% to ±2%)
                    BigDecimal basePrice = assetPrice.getCurrentPrice();
                    double variationPercent = (random.nextDouble() * 0.03 - 0.015); // -1.5% to +1.5%
                    BigDecimal variation = basePrice.multiply(BigDecimal.valueOf(variationPercent));
                    BigDecimal currentPrice = basePrice.add(variation);
                    return currentPrice.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
                })
                .orElseGet(() -> {
                    // If no price found, return buying rate as fallback
                    return assetRepository.findById(assetId)
                            .map(Asset::getBuyingRate)
                            .orElse(BigDecimal.ZERO);
                });
    }

    @Override
    @Scheduled(fixedRate = 21600000) // Every 6 hours
    @Transactional
    public void fetchAndUpdatePrices() {
        log.info("Starting scheduled price update...");
        List<Asset> assets = assetRepository.findAll();

        for (Asset asset : assets) {
            try {
                BigDecimal price = fetchPriceFromYFinance(asset);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    AssetPrice assetPrice = AssetPrice.builder()
                            .asset(asset)
                            .currentPrice(price)
                            .priceDate(Instant.now())
                            .source(AssetPrice.PriceSource.YFINANCE)
                            .build();
                    assetPriceRepository.save(assetPrice);
                }
            } catch (Exception e) {
                log.error("Failed to fetch price for asset {}: {}", asset.getId(), e.getMessage());
                // Use fake data as fallback
                BigDecimal fakePrice = generateFakePrice(asset);
                AssetPrice assetPrice = AssetPrice.builder()
                        .asset(asset)
                        .currentPrice(fakePrice)
                        .priceDate(Instant.now())
                        .source(AssetPrice.PriceSource.FAKE)
                        .build();
                assetPriceRepository.save(assetPrice);
            }
        }
        log.info("Price update completed");
    }

    private BigDecimal fetchPriceFromYFinance(Asset asset) {
        // For now, return fake data. In production, integrate with yfinance Python script
        // or use a Java library that wraps yfinance
        return generateFakePrice(asset);
    }

    private BigDecimal generateFakePrice(Asset asset) {
        BigDecimal basePrice = asset.getBuyingRate();
        // Simulate price movement: ±10% variation
        double variation = (random.nextDouble() * 0.2 - 0.1); // -10% to +10%
        BigDecimal newPrice = basePrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(variation)));
        return newPrice.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }
}
