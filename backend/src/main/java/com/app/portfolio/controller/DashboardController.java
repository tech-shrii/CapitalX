package com.app.portfolio.controller;

import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("GET /api/dashboard/summary - userId: {}", userPrincipal.getId());
        try {
            DashboardSummaryResponse response = dashboardService.getDashboardSummary(userPrincipal.getId());
            log.debug("Dashboard summary retrieved successfully for userId: {}", userPrincipal.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting dashboard summary for userId: {}", userPrincipal.getId(), e);
            throw e;
        }
    }

    @GetMapping("/clients/{clientId}/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getClientDashboardSummary(
            @PathVariable Long clientId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("GET /api/clients/{}/dashboard/summary - userId: {}", clientId, userPrincipal.getId());
        try {
            DashboardSummaryResponse response = dashboardService.getClientDashboardSummary(clientId, userPrincipal.getId());
            log.debug("Client dashboard summary retrieved successfully for clientId: {}, userId: {}", clientId, userPrincipal.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting client dashboard summary for clientId: {}, userId: {}", clientId, userPrincipal.getId(), e);
            throw e;
        }
    }
}
