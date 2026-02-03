package com.app.portfolio.service.dashboard;

import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary(Long userId);
    
    DashboardSummaryResponse getClientDashboardSummary(Long clientId, Long userId);
}
