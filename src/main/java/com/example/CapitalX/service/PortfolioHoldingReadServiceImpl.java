package com.example.CapitalX.service;

import com.example.CapitalX.beans.PortfolioHolding;
import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.dto.AssetPerformanceDTO;
import com.example.CapitalX.exceptions.PortfolioSnapshotNotFoundException;
import com.example.CapitalX.mapper.AssetPerformanceMapper;
import com.example.CapitalX.repository.PortfolioHoldingRepository;
import com.example.CapitalX.repository.PortfolioUploadRepository;
import com.example.CapitalX.beans.Asset.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioHoldingReadServiceImpl implements PortfolioHoldingReadService {
    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioUploadRepository uploadRepository;

    public PortfolioHoldingReadServiceImpl(PortfolioHoldingRepository holdingRepository,
                                          PortfolioUploadRepository uploadRepository) {
        this.holdingRepository = holdingRepository;
        this.uploadRepository = uploadRepository;
    }

    @Override
    public List<AssetPerformanceDTO> getHoldingsByUpload(Long uploadId) {
        List<PortfolioHolding> holdings = holdingRepository.findByUploadId(uploadId);
        return holdings.stream()
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssetPerformanceDTO> getHoldingsByCustomerLatest(Long customerId) {
        // Get latest upload for customer
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);
        if (uploads.isEmpty()) {
            throw new PortfolioSnapshotNotFoundException(
                "No portfolio snapshot found for customer: " + customerId);
        }

        Long latestUploadId = uploads.get(0).getUploadId();
        return getHoldingsByUpload(latestUploadId);
    }

    @Override
    public List<AssetPerformanceDTO> getActiveHoldings(Long customerId) {
        List<PortfolioHolding> holdings = holdingRepository.findByCustomerId(customerId);
        return holdings.stream()
            .filter(h -> h.getInvestmentEndDate() == null)
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssetPerformanceDTO> getExitedHoldings(Long customerId) {
        List<PortfolioHolding> holdings = holdingRepository.findByCustomerId(customerId);
        return holdings.stream()
            .filter(h -> h.getInvestmentEndDate() != null)
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssetPerformanceDTO> getProfitableHoldings(Long customerId) {
        List<PortfolioHolding> holdings = holdingRepository.findByCustomerId(customerId);
        return holdings.stream()
            .filter(h -> h.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssetPerformanceDTO> getLossHoldings(Long customerId) {
        List<PortfolioHolding> holdings = holdingRepository.findByCustomerId(customerId);
        return holdings.stream()
            .filter(h -> h.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssetPerformanceDTO> getHoldingsByAssetType(Long customerId, AssetType assetType) {
        List<PortfolioHolding> holdings = holdingRepository.findByCustomerId(customerId);
        return holdings.stream()
            .filter(h -> h.getAsset().getAssetType() == assetType)
            .map(AssetPerformanceMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }
}
