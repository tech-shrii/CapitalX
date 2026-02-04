package com.app.portfolio.controller;

import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.dashboard.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal mockUserPrincipal;

    @BeforeEach
    void setUp() {
        mockUserPrincipal = new UserPrincipal(1L, "test@example.com", "password", Collections.emptyList());
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor testUser() {
        return SecurityMockMvcRequestPostProcessors.user(mockUserPrincipal);
    }

    @Test
    void getDashboardSummary_shouldReturnSummary() throws Exception {
        Long userId = mockUserPrincipal.getId();
        DashboardSummaryResponse summaryResponse = DashboardSummaryResponse.builder()
                .totalClients(5L)
                .totalAssets(100L)
                .totalValue(new BigDecimal("123456.78"))
                .build();

        when(dashboardService.getDashboardSummary(eq(userId))).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/dashboard/summary")
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalClients").value(5))
                .andExpect(jsonPath("$.totalAssets").value(100))
                .andExpect(jsonPath("$.totalValue").value(123456.78));
    }

    @Test
    void getClientDashboardSummary_shouldReturnSummary() throws Exception {
        Long clientId = 1L;
        Long userId = mockUserPrincipal.getId();
        DashboardSummaryResponse summaryResponse = DashboardSummaryResponse.builder()
                .totalClients(1L)
                .totalAssets(20L)
                .totalValue(new BigDecimal("54321.99"))
                .build();

        when(dashboardService.getClientDashboardSummary(eq(clientId), eq(userId))).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/clients/{clientId}/dashboard/summary", clientId)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalClients").value(1))
                .andExpect(jsonPath("$.totalAssets").value(20))
                .andExpect(jsonPath("$.totalValue").value(54321.99));
    }
}
