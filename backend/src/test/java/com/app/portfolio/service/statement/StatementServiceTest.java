package com.app.portfolio.service.statement;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.statement.StatementRequest;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.service.asset.AssetService;
import com.app.portfolio.service.email.EmailService;
import com.app.portfolio.service.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Statement Service Tests")
class StatementServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private EmailService emailService;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private StatementServiceImpl statementService;

    private User testUser;
    private Client testClient;
    private Asset testAsset1;
    private Asset testAsset2;
    private StatementRequest statementRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testClient = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@example.com")
                .user(testUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset1 = Asset.builder()
                .id(1L)
                .name("Apple Inc.")
                .symbol("AAPL")
                .category(Asset.AssetCategory.STOCK)
                .quantity(new BigDecimal("100"))
                .buyingRate(new BigDecimal("150.00"))
                .purchaseDate(LocalDate.now().minusMonths(1))
                .client(testClient)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testAsset2 = Asset.builder()
                .id(2L)
                .name("Alphabet Inc.")
                .symbol("GOOGL")
                .category(Asset.AssetCategory.STOCK)
                .quantity(new BigDecimal("50"))
                .buyingRate(new BigDecimal("2500.00"))
                .purchaseDate(LocalDate.now().minusMonths(2))
                .client(testClient)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        statementRequest = new StatementRequest();
        statementRequest.setClientId(1L);
        statementRequest.setStatementType(StatementRequest.StatementType.BASIC_PNL);
        statementRequest.setEmailTo("recipient@example.com");
    }

    @Nested
    @DisplayName("Generate and Send Statement Tests")
    class GenerateAndSendStatementTests {

        @Test
        @DisplayName("Should generate and send statement when client exists and belongs to user")
        void shouldGenerateAndSendStatementWhenClientExistsAndBelongsToUser() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList(testAsset1, testAsset2));
            when(pricingService.getCurrentPrice(1L)).thenReturn(new BigDecimal("155.00"));
            when(pricingService.getCurrentPrice(2L)).thenReturn(new BigDecimal("2600.00"));

            statementService.generateAndSendStatement(statementRequest, 1L);

            verify(clientRepository).findById(1L);
            verify(assetRepository).findByClientIdOrderByPurchaseDateDesc(1L);
            verify(pricingService).getCurrentPrice(1L);
            verify(pricingService).getCurrentPrice(2L);
            verify(emailService).sendStatementEmail(
                    eq("recipient@example.com"),
                    eq("Test Client"),
                    any(byte[].class),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should use client email when emailTo is not provided")
        void shouldUseClientEmailWhenEmailToIsNotProvided() {
            statementRequest.setEmailTo(null);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList(testAsset1, testAsset2));
            when(pricingService.getCurrentPrice(1L)).thenReturn(new BigDecimal("155.00"));
            when(pricingService.getCurrentPrice(2L)).thenReturn(new BigDecimal("2600.00"));

            statementService.generateAndSendStatement(statementRequest, 1L);

            verify(emailService).sendStatementEmail(
                    eq("client@example.com"),
                    eq("Test Client"),
                    any(byte[].class),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientDoesNotExist() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.generateAndSendStatement(statementRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).findById(1L);
            verify(assetRepository, never()).findByClientIdOrderByPurchaseDateDesc(anyLong());
            verify(emailService, never()).sendStatementEmail(anyString(), anyString(), any(byte[].class), anyString());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not belong to user")
        void shouldThrowExceptionWhenClientDoesNotBelongToUser() {
            User otherUser = User.builder()
                    .id(2L)
                    .name("Other User")
                    .email("other@example.com")
                    .build();

            Client otherClient = Client.builder()
                    .id(1L)
                    .name("Other Client")
                    .email("otherclient@example.com")
                    .user(otherUser)
                    .build();

            when(clientRepository.findById(1L)).thenReturn(Optional.of(otherClient));

            assertThatThrownBy(() -> statementService.generateAndSendStatement(statementRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).findById(1L);
            verify(assetRepository, never()).findByClientIdOrderByPurchaseDateDesc(anyLong());
            verify(emailService, never()).sendStatementEmail(anyString(), anyString(), any(byte[].class), anyString());
        }

        @Test
        @DisplayName("Should generate detailed statement when statement type is DETAILED")
        void shouldGenerateDetailedStatementWhenStatementTypeIsDetailed() {
            statementRequest.setStatementType(StatementRequest.StatementType.DETAILED);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList(testAsset1, testAsset2));
            when(pricingService.getCurrentPrice(1L)).thenReturn(new BigDecimal("155.00"));
            when(pricingService.getCurrentPrice(2L)).thenReturn(new BigDecimal("2600.00"));

            statementService.generateAndSendStatement(statementRequest, 1L);

            verify(clientRepository).findById(1L);
            verify(assetRepository).findByClientIdOrderByPurchaseDateDesc(1L);
            verify(pricingService, times(2)).getCurrentPrice(anyLong());
            verify(emailService).sendStatementEmail(
                    anyString(),
                    eq("Test Client"),
                    any(byte[].class),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle empty asset list gracefully")
        void shouldHandleEmptyAssetListGracefully() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
            when(assetRepository.findByClientIdOrderByPurchaseDateDesc(1L))
                    .thenReturn(Arrays.asList());

            statementService.generateAndSendStatement(statementRequest, 1L);

            verify(clientRepository).findById(1L);
            verify(assetRepository).findByClientIdOrderByPurchaseDateDesc(1L);
            verify(pricingService, never()).getCurrentPrice(anyLong());
            verify(emailService).sendStatementEmail(
                    anyString(),
                    eq("Test Client"),
                    any(byte[].class),
                    anyString()
            );
        }
    }
}
