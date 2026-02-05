package com.app.portfolio.repository;

import com.app.portfolio.beans.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByClientIdOrderByPurchaseDateTimeDesc(Long clientId);

    boolean existsByIdAndClientUserId(Long assetId, Long userId);

    List<Asset> findBySymbolIn(List<String> symbols);
}
