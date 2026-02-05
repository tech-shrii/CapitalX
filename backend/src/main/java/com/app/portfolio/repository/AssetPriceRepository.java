package com.app.portfolio.repository;

import com.app.portfolio.beans.AssetPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetPriceRepository extends JpaRepository<AssetPrice, Long> {

    // Legacy method - kept for backward compatibility during migration
    Optional<AssetPrice> findFirstByAssetIdOrderByPriceDateDesc(Long assetId);

    // New symbol-based methods
    Optional<AssetPrice> findFirstBySymbolOrderByPriceDateDesc(String symbol);

    List<AssetPrice> findBySymbolOrderByPriceDateDesc(String symbol);

    // Migration helper: Update existing records to populate symbol from asset
    @Modifying
    @Query("UPDATE AssetPrice ap SET ap.symbol = :symbol WHERE ap.asset.id = :assetId AND ap.symbol IS NULL")
    int updateSymbolByAssetId(@Param("assetId") Long assetId, @Param("symbol") String symbol);

    // Migration helper: Get all asset prices that need symbol population
    @Query("SELECT ap FROM AssetPrice ap WHERE ap.symbol IS NULL AND ap.asset IS NOT NULL")
    List<AssetPrice> findAllWithoutSymbol();
    
    // Historical data queries for portfolio charts
    @Query("SELECT ap FROM AssetPrice ap WHERE ap.symbol = :symbol AND ap.priceDate >= :startDate AND ap.priceDate < :endDate ORDER BY ap.priceDate ASC")
    List<AssetPrice> findBySymbolAndDateRange(@Param("symbol") String symbol, 
                                                @Param("startDate") Instant startDate, 
                                                @Param("endDate") Instant endDate);
    
    // Historical data query filtered by MANUAL source for client charts
    @Query("SELECT ap FROM AssetPrice ap WHERE ap.symbol = :symbol AND ap.source = 'MANUAL' AND ap.priceDate >= :startDate AND ap.priceDate < :endDate ORDER BY ap.priceDate ASC")
    List<AssetPrice> findBySymbolAndDateRangeManual(@Param("symbol") String symbol, 
                                                      @Param("startDate") Instant startDate, 
                                                      @Param("endDate") Instant endDate);
    
    @Query("SELECT DISTINCT ap.symbol FROM AssetPrice ap WHERE ap.symbol IN :symbols")
    List<String> findDistinctSymbolsInList(@Param("symbols") List<String> symbols);
}
