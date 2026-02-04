// File: backend/src/test/java/com/app/portfolio/controller/AssetControllerTest.java
package com.app.portfolio.controller;

import com.app.portfolio.dto.asset.AssetRequest;
import com.app.portfolio.dto.asset.AssetResponse;
import com.app.portfolio.dto.asset.PnlResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.asset.AssetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private AssetController assetController;

    @Nested
    class ImportAssetsFromCSVTests {

        @org.junit.jupiter.api.Test
        @DisplayName("Import assets from CSV returns success for valid file")
        void importAssetsFromCSV_ReturnsSuccess_ForValidFile() throws Exception {
            long clientId = 1L;
            long userId = 2L;
            MockMultipartFile file = new MockMultipartFile("file", "assets.csv", "text/csv", "header,data".getBytes());
            when(userPrincipal.getId()).thenReturn(userId);
            when(assetService.importAssetsFromCSV(any(), eq(clientId), eq(userId))).thenReturn(3);

            ResponseEntity<Map<String, Object>> response = assetController.importAssetsFromCSV(clientId, file, userPrincipal);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("count", 3);
            assertThat(response.getBody()).containsEntry("message", "Assets imported successfully");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Import assets from CSV returns bad request for empty file")
        void importAssetsFromCSV_ReturnsBadRequest_ForEmptyFile() {
            long clientId = 1L;
            MockMultipartFile file = new MockMultipartFile("file", "assets.csv", "text/csv", new byte[0]);
            when(userPrincipal.getId()).thenReturn(2L);

            ResponseEntity<Map<String, Object>> response = assetController.importAssetsFromCSV(clientId, file, userPrincipal);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
            assertThat(response.getBody()).containsKey("error");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Import assets from CSV returns bad request for non-csv file")
        void importAssetsFromCSV_ReturnsBadRequest_ForNonCsvFile() {
            long clientId = 1L;
            MockMultipartFile file = new MockMultipartFile("file", "assets.txt", "text/plain", "data".getBytes());
            when(userPrincipal.getId()).thenReturn(2L);

            ResponseEntity<Map<String, Object>> response = assetController.importAssetsFromCSV(clientId, file, userPrincipal);

            assertThat(response.getStatusCodeValue()).isEqualTo(400);
            assertThat(response.getBody()).containsKey("error");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Import assets from CSV returns server error on IOException")
        void importAssetsFromCSV_ReturnsServerError_OnIOException() throws Exception {
            long clientId = 1L;
            MockMultipartFile file = mock(MockMultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("assets.csv");
            when(file.getSize()).thenReturn(10L);
            when(file.getInputStream()).thenThrow(new IOException("IO error"));
            when(userPrincipal.getId()).thenReturn(2L);

            ResponseEntity<Map<String, Object>> response = assetController.importAssetsFromCSV(clientId, file, userPrincipal);

            assertThat(response.getStatusCodeValue()).isEqualTo(500);
            assertThat(response.getBody()).containsKey("error");
        }

        @org.junit.jupiter.api.Test
        @DisplayName("Import assets from CSV returns server error on generic Exception")
        void importAssetsFromCSV_ReturnsServerError_OnGenericException() throws Exception {
            long clientId = 1L;
            MockMultipartFile file = mock(MockMultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("assets.csv");
            when(file.getSize()).thenReturn(10L);
            when(file.getInputStream()).thenThrow(new RuntimeException("Unexpected error"));
            when(userPrincipal.getId()).thenReturn(2L);

            ResponseEntity<Map<String, Object>> response = assetController.importAssetsFromCSV(clientId, file, userPrincipal);

            assertThat(response.getStatusCodeValue()).isEqualTo(500);
            assertThat(response.getBody()).containsKey("error");
        }
    }
}
