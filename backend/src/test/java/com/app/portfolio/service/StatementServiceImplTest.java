package com.app.portfolio.service;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.statement.StatementRequest;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.email.EmailService;
import com.app.portfolio.service.pricing.PricingService;
import com.app.portfolio.service.statement.StatementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatementServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private StatementServiceImpl statementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Generate and send statement with valid client and assets")
    void generateAndSendStatementWithValidClientAndAssets() {
        Client client = new Client();
        client.setId(1L);
        client.setName("John Doe");
        client.setEmail("john.doe@example.com");

        Asset asset = new Asset();
        asset.setId(1L);
        asset.setName("Asset 1");
        asset.setBuyingRate(BigDecimal.valueOf(100));
        asset.setQuantity(BigDecimal.valueOf(10));

        StatementRequest request = new StatementRequest();
        request.setClientId(1L);
        request.setStatementType(StatementRequest.StatementType.BASIC_PNL);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(List.of(asset));
        when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.valueOf(120));
        doNothing().when(emailService).sendStatementEmail(anyString(), anyString(), any(), anyString());

        assertDoesNotThrow(() -> statementService.generateAndSendStatement(request, 1L));
        verify(emailService).sendStatementEmail(eq("john.doe@example.com"), eq("John Doe"), any(), anyString());
    }

    @Test
    @DisplayName("Generate and send statement throws exception for invalid client")
    void generateAndSendStatementThrowsForInvalidClient() {
        StatementRequest request = new StatementRequest();
        request.setClientId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> statementService.generateAndSendStatement(request, 1L));
    }

    @Test
    @DisplayName("Generate and send statement throws exception for mismatched user ID")
    void generateAndSendStatementThrowsForMismatchedUserId() {
        Client client = new Client();
        client.setId(1L);
        client.setUser(new com.app.portfolio.beans.User());

        StatementRequest request = new StatementRequest();
        request.setClientId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThrows(ResourceNotFoundException.class, () -> statementService.generateAndSendStatement(request, 3L));
    }

    @Test
    @DisplayName("Generate and send statement with no assets")
    void generateAndSendStatementWithNoAssets() {
        Client client = new Client();
        client.setId(1L);
        client.setName("John Doe");
        client.setEmail("john.doe@example.com");

        StatementRequest request = new StatementRequest();
        request.setClientId(1L);
        request.setStatementType(StatementRequest.StatementType.BASIC_PNL);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L)).thenReturn(Collections.emptyList());
        doNothing().when(emailService).sendStatementEmail(anyString(), anyString(), any(), anyString());

        assertDoesNotThrow(() -> statementService.generateAndSendStatement(request, 1L));
        verify(emailService).sendStatementEmail(eq("john.doe@example.com"), eq("John Doe"), any(), anyString());
    }
}
