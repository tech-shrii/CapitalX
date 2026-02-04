package com.app.portfolio.controller;

import com.app.portfolio.dto.statement.StatementRequest;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.statement.StatementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class StatementControllerTest {

    @Mock
    private StatementService statementService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private StatementController statementController;

    @org.junit.jupiter.api.Test
    @DisplayName("Generate statement returns success message for valid request")
    void generateStatement_ReturnsSuccessMessage_ForValidRequest() {
        long userId = 1L;
        StatementRequest request = mock(StatementRequest.class);
        when(userPrincipal.getId()).thenReturn(userId);
        doNothing().when(statementService).generateAndSendStatement(request, userId);

        ResponseEntity<?> response = statementController.generateStatement(request, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(((Map<?, ?>) response.getBody()).get("message")).isEqualTo("Statement generated and sent successfully");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Generate statement throws exception when service throws exception")
    void generateStatement_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        StatementRequest request = mock(StatementRequest.class);
        when(userPrincipal.getId()).thenReturn(userId);
        doThrow(new RuntimeException("error")).when(statementService).generateAndSendStatement(request, userId);

        assertThrows(RuntimeException.class, () -> statementController.generateStatement(request, userPrincipal));
    }
}
