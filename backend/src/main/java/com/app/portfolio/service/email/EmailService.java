package com.app.portfolio.service.email;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otp, String purpose);

    void sendStatementEmail(String toEmail, String clientName, byte[] pdfAttachment, String fileName);

    void sendPasswordResetConfirmation(String toEmail);
}
