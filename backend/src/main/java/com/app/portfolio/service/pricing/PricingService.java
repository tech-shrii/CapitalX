package com.app.portfolio.service.pricing;

import java.math.BigDecimal;

public interface PricingService {

    BigDecimal getCurrentPrice(Long assetId);

    void fetchAndUpdatePrices();
}
