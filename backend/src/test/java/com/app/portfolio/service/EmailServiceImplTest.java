package com.app.portfolio.service;

import com.app.portfolio.service.email.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the fromEmail field via reflection since it's injected by @Value
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@portfolio.app");
    }

    @Test
    void sendOtpEmail_sendsEmailSuccessfully() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtpEmail("user@example.com", "123456", "Login");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOtpEmail_throwsRuntimeException_whenMessagingExceptionOccurs() throws Exception {
        when(mailSender.createMimeMessage()).thenThrow(new MessagingException("error"));

        assertThatThrownBy(() -> emailService.sendOtpEmail("user@example.com", "123456", "Login"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendStatementEmail_sendsEmailWithAttachmentSuccessfully() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendStatementEmail("user@example.com", "ClientName", new byte[]{1, 2, 3}, "statement.pdf");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendStatementEmail_throwsRuntimeException_whenMessagingExceptionOccurs() throws Exception {
        when(mailSender.createMimeMessage()).thenThrow(new MessagingException("error"));

        assertThatThrownBy(() -> emailService.sendStatementEmail("user@example.com", "ClientName", new byte[]{1}, "statement.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendPasswordResetConfirmation_sendsEmailSuccessfully() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetConfirmation("user@example.com");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetConfirmation_logsError_whenMessagingExceptionOccurs() throws Exception {
        when(mailSender.createMimeMessage()).thenThrow(new MessagingException("error"));

        // Should not throw, just log
        emailService.sendPasswordResetConfirmation("user@example.com");
    }
}
