package com.example.CapitalX.service;

import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.beans.PortfolioSummary;
import com.example.CapitalX.dto.PortfolioSnapshotDTO;
import com.example.CapitalX.exceptions.PortfolioSnapshotNotFoundException;
import com.example.CapitalX.mapper.PortfolioMapper;
import com.example.CapitalX.repository.PortfolioUploadRepository;
import com.example.CapitalX.repository.PortfolioSummaryRepository;
import com.example.CapitalX.beans.PortfolioUpload.PeriodType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioSnapshotServiceImpl implements PortfolioSnapshotService {
    private final PortfolioUploadRepository uploadRepository;
    private final PortfolioSummaryRepository summaryRepository;

    public PortfolioSnapshotServiceImpl(PortfolioUploadRepository uploadRepository,
                                        PortfolioSummaryRepository summaryRepository) {
        this.uploadRepository = uploadRepository;
        this.summaryRepository = summaryRepository;
    }

    @Override
    public PortfolioSnapshotDTO getLatestSnapshot(Long customerId) {
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);
        if (uploads.isEmpty()) {
            throw new PortfolioSnapshotNotFoundException(
                "No portfolio snapshot found for customer: " + customerId);
        }

        PortfolioUpload latestUpload = uploads.get(0);
        PortfolioSummary summary = summaryRepository.findByUploadId(latestUpload.getUploadId())
            .orElse(null);

        return PortfolioMapper.convertToSnapshotDTO(latestUpload, summary);
    }

    @Override
    public PortfolioSnapshotDTO getSnapshotByUploadId(Long uploadId) {
        PortfolioUpload upload = uploadRepository.findById(uploadId)
            .orElseThrow(() -> new PortfolioSnapshotNotFoundException(
                "Portfolio snapshot not found with id: " + uploadId));

        PortfolioSummary summary = summaryRepository.findByUploadId(uploadId).orElse(null);
        return PortfolioMapper.convertToSnapshotDTO(upload, summary);
    }

    @Override
    public List<PortfolioSnapshotDTO> getSnapshotsByCustomer(Long customerId) {
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);

        return uploads.stream()
            .map(upload -> {
                PortfolioSummary summary = summaryRepository.findByUploadId(upload.getUploadId())
                    .orElse(null);
                return PortfolioMapper.convertToSnapshotDTO(upload, summary);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioSnapshotDTO> getSnapshotsByPeriod(Long customerId, PeriodType periodType) {
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);

        return uploads.stream()
            .filter(upload -> upload.getPeriodType() == periodType)
            .map(upload -> {
                PortfolioSummary summary = summaryRepository.findByUploadId(upload.getUploadId())
                    .orElse(null);
                return PortfolioMapper.convertToSnapshotDTO(upload, summary);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioSnapshotDTO> getSnapshotsByYear(Long customerId, Integer year) {
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);

        return uploads.stream()
            .filter(upload -> upload.getUploadDate().getYear() == year)
            .map(upload -> {
                PortfolioSummary summary = summaryRepository.findByUploadId(upload.getUploadId())
                    .orElse(null);
                return PortfolioMapper.convertToSnapshotDTO(upload, summary);
            })
            .collect(Collectors.toList());
    }
}
