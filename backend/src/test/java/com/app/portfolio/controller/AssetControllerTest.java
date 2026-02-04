package com.app.portfolio.controller;

import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.asset.AssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
public class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService assetService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal mockUserPrincipal;

    @BeforeEach
    void setUp() {
        mockUserPrincipal = new UserPrincipal(1L, "test@example.com", "password", Collections.emptyList());
    }

    // Helper to simulate @AuthenticationPrincipal UserPrincipal
    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor testUser() {
        return SecurityMockMvcRequestPostProcessors.user(mockUserPrincipal);
    }

    @Test
    @WithMockUser // This is just for Spring Security context, actual UserPrincipal is mocked in assetService calls
    void getAssetById_shouldReturnAsset_whenFound() throws Exception {
        Long assetId = 1L;
        Long userId = mockUserPrincipal.getId();
        AssetResponse expectedResponse = AssetResponse.builder()
                .id(assetId)
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("10.00"))
                .buyingRate(new BigDecimal("150.00"))
                .category("STOCK")
                .purchaseDate(LocalDate.of(2023, 1, 1))
                .currency("USD")
                .currentPrice(new BigDecimal("160.00"))
                .profitLoss(new BigDecimal("100.00"))
                .profitLossPercent(new BigDecimal("6.67"))
                .build();

        when(assetService.getAssetById(eq(assetId), eq(userId))).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/assets/{id}", assetId)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(assetId))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.quantity").value(10.00))
                .andExpect(jsonPath("$.buyingRate").value(150.00))
                .andExpect(jsonPath("$.category").value("STOCK"))
                .andExpect(jsonPath("$.purchaseDate").value("2023-01-01"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.currentPrice").value(160.00))
                .andExpect(jsonPath("$.profitLoss").value(100.00))
                .andExpect(jsonPath("$.profitLossPercent").value(6.67));
    }

    @Test
    @WithMockUser
    void getAssetById_shouldReturnNotFound_whenAssetNotFound() throws Exception {
        Long assetId = 99L; // An ID that won't be found
        Long userId = mockUserPrincipal.getId();

        when(assetService.getAssetById(eq(assetId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("Asset", assetId));

        mockMvc.perform(get("/api/assets/{id}", assetId)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Asset not found with id '99'"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    void importAssetsFromCSV_shouldReturnBadRequest_whenCsvMissingRequiredColumn() throws Exception {
        Long clientId = 1L;
        Long userId = mockUserPrincipal.getId();
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "symbol,quantity,bad_column\nAAPL,10,some_value".getBytes() // Missing buyingRate
        );

        when(assetService.importAssetsFromCSV(any(java.io.InputStream.class), eq(clientId), eq(userId)))
                .thenThrow(new IllegalArgumentException("CSV must have column: buyingRate"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/clients/{clientId}/assets/import-csv", clientId)
                        .file(csvFile)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("CSV must have column: buyingRate"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    void importAssetsFromCSV_shouldReturnNotFound_whenClientNotFound() throws Exception {
        Long clientId = 99L; // An ID that won't be found
        Long userId = mockUserPrincipal.getId();
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "symbol,quantity\nAAPL,10".getBytes()
        );

        when(assetService.importAssetsFromCSV(any(java.io.InputStream.class), eq(clientId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("Client", clientId));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/clients/{clientId}/assets/import-csv", clientId)
                        .file(csvFile)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Client not found with id '99'"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
