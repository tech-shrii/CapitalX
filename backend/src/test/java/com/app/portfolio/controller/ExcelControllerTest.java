package com.app.portfolio.controller;

import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.excel.ExcelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ExcelControllerTest {

    @Mock
    private ExcelService excelService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private ExcelController excelController;

    @Mock
    private MultipartFile multipartFile;

    @org.junit.jupiter.api.Test
    @DisplayName("Import Excel returns success message for valid file")
    void importExcel_ReturnsSuccessMessage_ForValidFile() throws IOException {
        long userId = 1L;
        when(userPrincipal.getId()).thenReturn(userId);
        doNothing().when(excelService).importFromExcel(multipartFile, userId);

        ResponseEntity<?> response = excelController.importExcel(multipartFile, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(((java.util.Map<?, ?>) response.getBody()).get("message")).isEqualTo("Excel imported successfully");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Import Excel throws exception when service throws exception")
    void importExcel_ThrowsException_WhenServiceThrowsException() throws IOException {
        long userId = 1L;
        when(userPrincipal.getId()).thenReturn(userId);
        doThrow(new IOException("error")).when(excelService).importFromExcel(multipartFile, userId);

        assertThrows(IOException.class, () -> excelController.importExcel(multipartFile, userPrincipal));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Export Excel returns file with correct headers and body")
    void exportExcel_ReturnsFileWithCorrectHeadersAndBody() throws IOException {
        long userId = 1L;
        byte[] excelBytes = new byte[]{1, 2, 3};
        when(userPrincipal.getId()).thenReturn(userId);
        when(excelService.exportToExcel(userId)).thenReturn(excelBytes);

        ResponseEntity<byte[]> response = excelController.exportExcel(userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(response.getBody()).isEqualTo(excelBytes);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Export Excel throws exception when service throws exception")
    void exportExcel_ThrowsException_WhenServiceThrowsException() throws IOException {
        long userId = 1L;
        when(userPrincipal.getId()).thenReturn(userId);
        when(excelService.exportToExcel(userId)).thenThrow(new IOException("error"));

        assertThrows(IOException.class, () -> excelController.exportExcel(userPrincipal));
    }
}
