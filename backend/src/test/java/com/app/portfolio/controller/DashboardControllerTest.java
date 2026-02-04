package com.app.portfolio.controller;

import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.dashboard.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private DashboardController dashboardController;

    @org.junit.jupiter.api.Test
    @DisplayName("Get dashboard summary returns summary for valid user")
    void getDashboardSummary_ReturnsSummary_ForValidUser() {
        long userId = 1L;
        DashboardSummaryResponse summary = mock(DashboardSummaryResponse.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(dashboardService.getDashboardSummary(userId)).thenReturn(summary);

        ResponseEntity<DashboardSummaryResponse> response = dashboardController.getDashboardSummary(userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(summary);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get dashboard summary throws exception when service throws exception")
    void getDashboardSummary_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        when(userPrincipal.getId()).thenReturn(userId);
        when(dashboardService.getDashboardSummary(userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> dashboardController.getDashboardSummary(userPrincipal));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get client dashboard summary returns summary for valid client and user")
    void getClientDashboardSummary_ReturnsSummary_ForValidClientAndUser() {
        long userId = 1L;
        long clientId = 2L;
        DashboardSummaryResponse summary = mock(DashboardSummaryResponse.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(dashboardService.getClientDashboardSummary(clientId, userId)).thenReturn(summary);

        ResponseEntity<DashboardSummaryResponse> response = dashboardController.getClientDashboardSummary(clientId, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(summary);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get client dashboard summary throws exception when service throws exception")
    void getClientDashboardSummary_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        long clientId = 2L;
        when(userPrincipal.getId()).thenReturn(userId);
        when(dashboardService.getClientDashboardSummary(clientId, userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> dashboardController.getClientDashboardSummary(clientId, userPrincipal));
    }
}
