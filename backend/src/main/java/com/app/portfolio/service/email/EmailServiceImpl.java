package com.app.portfolio.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@portfolio.app}")
    private String fromEmail;

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your OTP for Portfolio Dashboard - " + purpose);
            helper.setText("Your one-time password is: <strong>" + otp + "</strong>. It expires in 10 minutes. Do not share this code.", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    @Async
    public void sendStatementEmail(String toEmail, String clientName, byte[] pdfAttachment, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Portfolio Statement - " + clientName);
            helper.setText("Please find your portfolio statement attached.");
            helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(pdfAttachment));
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send statement email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    @Async
    public void sendPasswordResetConfirmation(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Successful - Portfolio Dashboard");
            helper.setText("Your password has been reset successfully. If you did not make this change, please contact support.", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send password reset confirmation to {}", toEmail, e);
        }
    }
}
