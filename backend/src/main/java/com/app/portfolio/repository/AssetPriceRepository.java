package com.app.portfolio.repository;

import com.app.portfolio.beans.AssetPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetPriceRepository extends JpaRepository<AssetPrice, Long> {

    Optional<AssetPrice> findFirstByAssetIdOrderByPriceDateDesc(Long assetId);
}
