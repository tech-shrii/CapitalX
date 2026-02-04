package com.app.portfolio.service;

import com.app.portfolio.service.email.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Send OTP email successfully")
    void sendOtpEmail_sendsEmailSuccessfully() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail("user@example.com", "123456", "Login");

        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("Send OTP email throws RuntimeException on MessagingException")
    void sendOtpEmail_throwsRuntimeException_whenMessagingExceptionOccurs() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> emailService.sendOtpEmail("user@example.com", "123456", "Login"));
    }

    @Test
    @DisplayName("Send statement email with attachment successfully")
    void sendStatementEmail_sendsEmailWithAttachmentSuccessfully() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendStatementEmail("user@example.com", "ClientName", new byte[]{1, 2, 3}, "statement.pdf");

        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("Send statement email handles empty attachment gracefully")
    void sendStatementEmail_handlesEmptyAttachmentGracefully() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendStatementEmail("user@example.com", "ClientName", new byte[0], "statement.pdf");

        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("Send password reset confirmation email successfully")
    void sendPasswordResetConfirmation_sendsEmailSuccessfully() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendPasswordResetConfirmation("user@example.com");

        verify(mailSender).send(message);
    }

    @Test
    @DisplayName("Send password reset confirmation logs error on MessagingException")
    void sendPasswordResetConfirmation_logsError_whenMessagingExceptionOccurs() {
        doThrow(new RuntimeException("error")).when(mailSender).createMimeMessage();

        assertDoesNotThrow(() -> emailService.sendPasswordResetConfirmation("user@example.com"));
    }

    @Test
    @DisplayName("Send OTP email does not throw exception for valid inputs")
    void sendOtpEmail_doesNotThrowExceptionForValidInputs() {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertDoesNotThrow(() -> emailService.sendOtpEmail("user@example.com", "123456", "Login"));
    }

    @Test
    @DisplayName("Send statement email does not throw exception for valid inputs")
    void sendStatementEmail_doesNotThrowExceptionForValidInputs() {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertDoesNotThrow(() -> emailService.sendStatementEmail("user@example.com", "ClientName", new byte[]{1, 2, 3}, "statement.pdf"));
    }
}
