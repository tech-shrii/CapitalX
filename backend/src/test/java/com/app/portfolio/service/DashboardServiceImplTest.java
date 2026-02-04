package com.app.portfolio.service;

import com.app.portfolio.beans.*;
import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.dto.pricing.PortfolioChartDataPoint;
import com.app.portfolio.dto.pricing.PortfolioChartResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.*;
import com.app.portfolio.service.dashboard.DashboardServiceImpl;
import com.app.portfolio.service.pricing.PricingService;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceImplTest {
    @Mock private ClientRepository clientRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private PricingService pricingService;

    @InjectMocks private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDashboardSummary_shouldReturnSummary_whenUserHasClientsAndAssets() {
        Client client = new Client();
        client.setId(1L);
        client.setName("Client1");
        client.setEmail("c1@e.com");
        List<Client> clients = List.of(client);

        Asset asset = new Asset();
        asset.setId(2L);
        asset.setSymbol("AAPL");
        asset.setBuyingRate(BigDecimal.TEN);
        asset.setQuantity(BigDecimal.ONE);
        asset.setCategory(Asset.AssetCategory.STOCK);
        asset.setName("Apple");
        asset.setClient(client);

        when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(clients);
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(List.of(asset));
        when(pricingService.getCurrentPrice(2L)).thenReturn(BigDecimal.valueOf(12));

        List<PortfolioChartDataPoint> oneDayData = new ArrayList<>();
        PortfolioChartDataPoint dp1 = new PortfolioChartDataPoint();
        dp1.setValue(100.0);
        oneDayData.add(dp1);
        PortfolioChartDataPoint dp2 = new PortfolioChartDataPoint();
        dp2.setValue(110.0);
        oneDayData.add(dp2);
        PortfolioChartResponse oneDayResponse = new PortfolioChartResponse();
        oneDayResponse.setData(oneDayData);

        List<PortfolioChartDataPoint> sixMonthData = new ArrayList<>();
        PortfolioChartDataPoint dp3 = new PortfolioChartDataPoint();
        dp3.setTime("T1");
        dp3.setValue(100.0);
        sixMonthData.add(dp3);
        PortfolioChartDataPoint dp4 = new PortfolioChartDataPoint();
        dp4.setTime("T2");
        dp4.setValue(110.0);
        sixMonthData.add(dp4);
        PortfolioChartResponse sixMonthResponse = new PortfolioChartResponse();
        sixMonthResponse.setData(sixMonthData);

        when(pricingService.getPortfolioChart(any(), eq("1d"), eq("1d"))).thenReturn(oneDayResponse);
        when(pricingService.getPortfolioChart(any(), eq("6m"), eq("1wk"))).thenReturn(sixMonthResponse);

        DashboardSummaryResponse response = dashboardService.getDashboardSummary(1L);

        assertThat(response.getTotalClients()).isEqualTo(1L);
        assertThat(response.getTotalAssets()).isEqualTo(1L);
        assertThat(response.getTotalInvested()).isEqualTo(BigDecimal.TEN);
        assertThat(response.getTotalCurrentValue()).isEqualTo(BigDecimal.valueOf(12));
        assertThat(response.getTopAssets()).isNotEmpty();
        assertThat(response.getWorstAssets()).isNotEmpty();
        assertThat(response.getPortfolioPerformance().getLabels()).contains("T1", "T2");
    }

    @Test
    void getDashboardSummary_shouldReturnEmptySummary_whenNoClients() {
        when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(1L);
        assertThat(response.getTotalClients()).isZero();
        assertThat(response.getTotalAssets()).isZero();
    }

    @Test
    void getClientDashboardSummary_shouldThrow_whenClientNotBelongToUser() {
        when(clientRepository.existsByIdAndUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> dashboardService.getClientDashboardSummary(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
