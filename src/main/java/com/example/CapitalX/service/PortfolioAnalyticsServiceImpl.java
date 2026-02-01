//package com.example.CapitalX.service;
//
//import com.example.CapitalX.beans.AnnualPerformance;
//import com.example.CapitalX.beans.Customer;
//import com.example.CapitalX.beans.PortfolioHolding;
//import com.example.CapitalX.dto.PortfolioAnalyticsDTO;
//import com.example.CapitalX.dto.CustomerPerformanceDTO;
//import com.example.CapitalX.dto.AssetExposureDTO;
//import com.example.CapitalX.repository.AnnualPerformanceRepository;
//import com.example.CapitalX.repository.CustomerRepository;
//import com.example.CapitalX.repository.PortfolioHoldingRepository;
//import com.example.CapitalX.repository.PortfolioSummaryRepository;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class PortfolioAnalyticsServiceImpl implements PortfolioAnalyticsService {
//    private final CustomerRepository customerRepository;
//    private final PortfolioHoldingRepository holdingRepository;
//    private final PortfolioSummaryRepository summaryRepository;
//    private final AnnualPerformanceRepository performanceRepository;
//
//    public PortfolioAnalyticsServiceImpl(CustomerRepository customerRepository,
//                                        PortfolioHoldingRepository holdingRepository,
//                                        PortfolioSummaryRepository summaryRepository,
//                                        AnnualPerformanceRepository performanceRepository) {
//        this.customerRepository = customerRepository;
//        this.holdingRepository = holdingRepository;
//        this.summaryRepository = summaryRepository;
//        this.performanceRepository = performanceRepository;
//    }
//
//    @Override
//    public PortfolioAnalyticsDTO getPortfolioManagerOverview() {
//        PortfolioAnalyticsDTO overview = new PortfolioAnalyticsDTO();
//
//        // Aggregate portfolio metrics
//        List<Customer> customers = customerRepository.findAll();
//
//        BigDecimal totalValue = BigDecimal.ZERO;
//        BigDecimal totalInvested = BigDecimal.ZERO;
//        BigDecimal totalProfitLoss = BigDecimal.ZERO;
//
//        // Collect all customer performance
//        List<CustomerPerformanceDTO> allCustomerPerformance = new ArrayList<>();
//        Set<Long> allAssets = new HashSet<>();
//
//        for (Customer customer : customers) {
//            // Get latest summary for each customer
//            List<AnnualPerformance> latestPerformance = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());
//
//            if (!latestPerformance.isEmpty()) {
//                AnnualPerformance perf = latestPerformance.get(0);
//                BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
//                    ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
//                    : BigDecimal.ZERO;
//
//                CustomerPerformanceDTO custPerf = new CustomerPerformanceDTO(
//                    customer.getCustomerId(),
//                    customer.getCustomerCode(),
//                    customer.getCustomerName(),
//                    perf.getClosingValue(),
//                    perf.getTotalProfitLoss(),
//                    returnPercentage,
//                    0 // Will update with asset count
//                );
//                allCustomerPerformance.add(custPerf);
//
//                totalValue = totalValue.add(perf.getClosingValue());
//                totalInvested = totalInvested.add(perf.getTotalInvestedDuringYear());
//                totalProfitLoss = totalProfitLoss.add(perf.getTotalProfitLoss());
//            }
//        }
//
//        // Count unique assets
//        List<PortfolioHolding> allHoldings = holdingRepository.findAll();
//        allAssets.addAll(allHoldings.stream()
//            .map(h -> h.getAsset().getAssetId())
//            .collect(Collectors.toSet()));
//
//        overview.setTotalPortfolioValue(totalValue);
//        overview.setTotalInvestedValue(totalInvested);
//        overview.setTotalProfitLoss(totalProfitLoss);
//        overview.setNumberOfCustomers((long) customers.size());
//        overview.setNumberOfAssets((long) allAssets.size());
//        overview.setTopPerformingCustomers(getTopPerformingCustomers(5));
//        overview.setBottomPerformingCustomers(getBottomPerformingCustomers(5));
//        overview.setTopAssets(getTopAssets(5));
//        overview.setRiskAssets(getRiskyAssets(5));
//
//        return overview;
//    }
//
//    @Override
//    public PortfolioAnalyticsDTO getManagerOverviewByYear(Integer year) {
//        // Similar to above but filtered by year
//        return getPortfolioManagerOverview();
//    }
//
//    @Override
//    public List<CustomerPerformanceDTO> getTopPerformingCustomers(Integer limit) {
//        List<Customer> customers = customerRepository.findAll();
//
//        return customers.stream()
//            .map(customer -> {
//                List<AnnualPerformance> performances = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());
//                if (performances.isEmpty()) {
//                    return null;
//                }
//                AnnualPerformance perf = performances.get(0);
//                BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
//                    ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
//                    : BigDecimal.ZERO;
//
//                return new CustomerPerformanceDTO(
//                    customer.getCustomerId(),
//                    customer.getCustomerCode(),
//                    customer.getCustomerName(),
//                    perf.getClosingValue(),
//                    perf.getTotalProfitLoss(),
//                    returnPercentage,
//                    0
//                );
//            })
//            .filter(Objects::nonNull)
//            .sorted(Comparator.comparing(CustomerPerformanceDTO::getReturnPercentage).reversed())
//            .limit(limit)
//            .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<CustomerPerformanceDTO> getBottomPerformingCustomers(Integer limit) {
//        List<Customer> customers = customerRepository.findAll();
//
//        return customers.stream()
//            .map(customer -> {
//                List<AnnualPerformance> performances = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());
//                if (performances.isEmpty()) {
//                    return null;
//                }
//                AnnualPerformance perf = performances.get(0);
//                BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
//                    ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
//                    : BigDecimal.ZERO;
//
//                return new CustomerPerformanceDTO(
//                    customer.getCustomerId(),
//                    customer.getCustomerCode(),
//                    customer.getCustomerName(),
//                    perf.getClosingValue(),
//                    perf.getTotalProfitLoss(),
//                    returnPercentage,
//                    0
//                );
//            })
//            .filter(Objects::nonNull)
//            .sorted(Comparator.comparing(CustomerPerformanceDTO::getReturnPercentage))
//            .limit(limit)
//            .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<AssetExposureDTO> getTopAssets(Integer limit) {
//        List<PortfolioHolding> allHoldings = holdingRepository.findAll();
//
//        return allHoldings.stream()
//            .collect(Collectors.groupingBy(
//                h -> h.getAsset().getAssetId(),
//                Collectors.summarizingBigDecimal(PortfolioHolding::getCurrentValue)
//            ))
//            .entrySet().stream()
//            .map(entry -> {
//                PortfolioHolding sample = allHoldings.stream()
//                    .filter(h -> h.getAsset().getAssetId().equals(entry.getKey()))
//                    .findFirst()
//                    .orElse(null);
//
//                if (sample == null) return null;
//
//                return new AssetExposureDTO(
//                    sample.getAsset().getAssetId(),
//                    sample.getAsset().getAssetCode(),
//                    sample.getAsset().getAssetName(),
//                    sample.getAsset().getAssetType().toString(),
//                    entry.getValue().getSum(),
//                    (long) allHoldings.stream().filter(h -> h.getAsset().getAssetId().equals(entry.getKey())).count(),
//                    BigDecimal.ZERO,
//                    "LOW"
//                );
//            })
//            .filter(Objects::nonNull)
//            .sorted(Comparator.comparing(AssetExposureDTO::getTotalValue).reversed())
//            .limit(limit)
//            .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<AssetExposureDTO> getRiskyAssets(Integer limit) {
//        List<PortfolioHolding> allHoldings = holdingRepository.findAll();
//
//        return allHoldings.stream()
//            .collect(Collectors.groupingBy(
//                h -> h.getAsset().getAssetId(),
//                Collectors.summarizingBigDecimal(PortfolioHolding::getProfitLoss)
//            ))
//            .entrySet().stream()
//            .map(entry -> {
//                PortfolioHolding sample = allHoldings.stream()
//                    .filter(h -> h.getAsset().getAssetId().equals(entry.getKey()))
//                    .findFirst()
//                    .orElse(null);
//
//                if (sample == null) return null;
//
//                String riskLevel = entry.getValue().getSum().compareTo(BigDecimal.ZERO) < 0 ? "HIGH" : "LOW";
//
//                return new AssetExposureDTO(
//                    sample.getAsset().getAssetId(),
//                    sample.getAsset().getAssetCode(),
//                    sample.getAsset().getAssetName(),
//                    sample.getAsset().getAssetType().toString(),
//                    entry.getValue().getSum(),
//                    (long) allHoldings.stream().filter(h -> h.getAsset().getAssetId().equals(entry.getKey())).count(),
//                    BigDecimal.ZERO,
//                    riskLevel
//                );
//            })
//            .filter(Objects::nonNull)
//            .filter(a -> "HIGH".equals(a.getRiskLevel()))
//            .sorted(Comparator.comparing(AssetExposureDTO::getTotalValue))
//            .limit(limit)
//            .collect(Collectors.toList());
//    }
//}
package com.example.CapitalX.service;

import com.example.CapitalX.beans.AnnualPerformance;
import com.example.CapitalX.beans.Asset;
import com.example.CapitalX.beans.Customer;
import com.example.CapitalX.beans.PortfolioHolding;
import com.example.CapitalX.dto.PortfolioAnalyticsDTO;
import com.example.CapitalX.dto.CustomerPerformanceDTO;
import com.example.CapitalX.dto.AssetExposureDTO;
import com.example.CapitalX.repository.AnnualPerformanceRepository;
import com.example.CapitalX.repository.CustomerRepository;
import com.example.CapitalX.repository.PortfolioHoldingRepository;
import com.example.CapitalX.repository.PortfolioSummaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioAnalyticsServiceImpl implements PortfolioAnalyticsService {
    private final CustomerRepository customerRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioSummaryRepository summaryRepository;
    private final AnnualPerformanceRepository performanceRepository;

    public PortfolioAnalyticsServiceImpl(CustomerRepository customerRepository,
                                         PortfolioHoldingRepository holdingRepository,
                                         PortfolioSummaryRepository summaryRepository,
                                         AnnualPerformanceRepository performanceRepository) {
        this.customerRepository = customerRepository;
        this.holdingRepository = holdingRepository;
        this.summaryRepository = summaryRepository;
        this.performanceRepository = performanceRepository;
    }

    @Override
    public PortfolioAnalyticsDTO getPortfolioManagerOverview() {
        PortfolioAnalyticsDTO overview = new PortfolioAnalyticsDTO();

        List<Customer> customers = customerRepository.findAll();

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        List<CustomerPerformanceDTO> allCustomerPerformance = new ArrayList<>();
        Set<Long> allAssets = new HashSet<>();

        for (Customer customer : customers) {
            List<AnnualPerformance> latestPerformance = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());

            if (!latestPerformance.isEmpty()) {
                AnnualPerformance perf = latestPerformance.get(0);
                BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
                        ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                CustomerPerformanceDTO custPerf = new CustomerPerformanceDTO(
                        customer.getCustomerId(),
                        customer.getCustomerCode(),
                        customer.getCustomerName(),
                        perf.getClosingValue(),
                        perf.getTotalProfitLoss(),
                        returnPercentage,
                        0
                );
                allCustomerPerformance.add(custPerf);

                totalValue = totalValue.add(perf.getClosingValue());
                totalInvested = totalInvested.add(perf.getTotalInvestedDuringYear());
                totalProfitLoss = totalProfitLoss.add(perf.getTotalProfitLoss());
            }
        }

        List<PortfolioHolding> allHoldings = holdingRepository.findAll();
        allAssets.addAll(allHoldings.stream()
                .map(h -> h.getAsset().getAssetId())
                .collect(Collectors.toSet()));

        overview.setTotalPortfolioValue(totalValue);
        overview.setTotalInvestedValue(totalInvested);
        overview.setTotalProfitLoss(totalProfitLoss);
        overview.setNumberOfCustomers((long) customers.size());
        overview.setNumberOfAssets((long) allAssets.size());
        overview.setTopPerformingCustomers(getTopPerformingCustomers(5));
        overview.setBottomPerformingCustomers(getBottomPerformingCustomers(5));
        overview.setTopAssets(getTopAssets(5));
        overview.setRiskAssets(getRiskyAssets(5));

        return overview;
    }

    @Override
    public PortfolioAnalyticsDTO getManagerOverviewByYear(Integer year) {
        return getPortfolioManagerOverview();
    }

    @Override
    public List<CustomerPerformanceDTO> getTopPerformingCustomers(Integer limit) {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customer -> {
                    List<AnnualPerformance> performances = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());
                    if (performances.isEmpty()) {
                        return null;
                    }
                    AnnualPerformance perf = performances.get(0);
                    BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
                            ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return new CustomerPerformanceDTO(
                            customer.getCustomerId(),
                            customer.getCustomerCode(),
                            customer.getCustomerName(),
                            perf.getClosingValue(),
                            perf.getTotalProfitLoss(),
                            returnPercentage,
                            0
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CustomerPerformanceDTO::getReturnPercentage).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerPerformanceDTO> getBottomPerformingCustomers(Integer limit) {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customer -> {
                    List<AnnualPerformance> performances = performanceRepository.findByCustomerIdOrderByFinancialYearDesc(customer.getCustomerId());
                    if (performances.isEmpty()) {
                        return null;
                    }
                    AnnualPerformance perf = performances.get(0);
                    BigDecimal returnPercentage = perf.getOpeningValue().compareTo(BigDecimal.ZERO) > 0
                            ? perf.getTotalProfitLoss().divide(perf.getOpeningValue(), 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return new CustomerPerformanceDTO(
                            customer.getCustomerId(),
                            customer.getCustomerCode(),
                            customer.getCustomerName(),
                            perf.getClosingValue(),
                            perf.getTotalProfitLoss(),
                            returnPercentage,
                            0
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CustomerPerformanceDTO::getReturnPercentage))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetExposureDTO> getTopAssets(Integer limit) {
        List<PortfolioHolding> allHoldings = holdingRepository.findAll();

        return allHoldings.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getAsset().getAssetId(),
                        Collectors.reducing(BigDecimal.ZERO, PortfolioHolding::getCurrentValue, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> {
                    PortfolioHolding sample = allHoldings.stream()
                            .filter(h -> h.getAsset().getAssetId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    if (sample == null) return null;

                    Asset asset = sample.getAsset();
                    return new AssetExposureDTO(
                            asset.getAssetId(),
                            asset.getAssetCode(),
                            asset.getAssetName(),
                            asset.getAssetType().toString(),
                            entry.getValue(),
                            (long) allHoldings.stream().filter(h -> h.getAsset().getAssetId().equals(entry.getKey())).count(),
                            BigDecimal.ZERO,
                            "LOW"
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AssetExposureDTO::getTotalValue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetExposureDTO> getRiskyAssets(Integer limit) {
        List<PortfolioHolding> allHoldings = holdingRepository.findAll();

        return allHoldings.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getAsset().getAssetId(),
                        Collectors.reducing(BigDecimal.ZERO, PortfolioHolding::getProfitLoss, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> {
                    PortfolioHolding sample = allHoldings.stream()
                            .filter(h -> h.getAsset().getAssetId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    if (sample == null) return null;

                    Asset asset = sample.getAsset();
                    String riskLevel = entry.getValue().compareTo(BigDecimal.ZERO) < 0 ? "HIGH" : "LOW";

                    return new AssetExposureDTO(
                            asset.getAssetId(),
                            asset.getAssetCode(),
                            asset.getAssetName(),
                            asset.getAssetType().toString(),
                            entry.getValue(),
                            (long) allHoldings.stream().filter(h -> h.getAsset().getAssetId().equals(entry.getKey())).count(),
                            BigDecimal.ZERO,
                            riskLevel
                    );
                })
                .filter(Objects::nonNull)
                .filter(a -> "HIGH".equals(a.getRiskLevel()))
                .sorted(Comparator.comparing(AssetExposureDTO::getTotalValue))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
