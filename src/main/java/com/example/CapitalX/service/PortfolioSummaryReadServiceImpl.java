package com.example.CapitalX.service;

import com.example.CapitalX.beans.PortfolioSummary;
import com.example.CapitalX.beans.PortfolioUpload;
import com.example.CapitalX.dto.PortfolioSnapshotDTO;
import com.example.CapitalX.exceptions.PortfolioSnapshotNotFoundException;
import com.example.CapitalX.mapper.PortfolioMapper;
import com.example.CapitalX.repository.PortfolioSummaryRepository;
import com.example.CapitalX.repository.PortfolioUploadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioSummaryReadServiceImpl implements PortfolioSummaryReadService {
    private final PortfolioSummaryRepository summaryRepository;
    private final PortfolioUploadRepository uploadRepository;

    public PortfolioSummaryReadServiceImpl(PortfolioSummaryRepository summaryRepository,
                                          PortfolioUploadRepository uploadRepository) {
        this.summaryRepository = summaryRepository;
        this.uploadRepository = uploadRepository;
    }

    @Override
    public PortfolioSnapshotDTO getPortfolioSummary(Long uploadId) {
        PortfolioUpload upload = uploadRepository.findById(uploadId)
            .orElseThrow(() -> new PortfolioSnapshotNotFoundException(
                "Portfolio snapshot not found with id: " + uploadId));

        PortfolioSummary summary = summaryRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new PortfolioSnapshotNotFoundException(
                "Portfolio summary not found for upload: " + uploadId));

        return PortfolioMapper.convertToSnapshotDTO(upload, summary);
    }

    @Override
    public List<PortfolioSnapshotDTO> compareSnapshots(Long customerId) {
        List<PortfolioUpload> uploads = uploadRepository.findByCustomerIdOrderByUploadDateDesc(customerId);

        return uploads.stream()
            .map(upload -> {
                PortfolioSummary summary = summaryRepository.findByUploadId(upload.getUploadId())
                    .orElse(null);
                return PortfolioMapper.convertToSnapshotDTO(upload, summary);
            })
            .collect(Collectors.toList());
    }
}
