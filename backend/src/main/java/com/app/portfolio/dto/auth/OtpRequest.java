package com.app.portfolio.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6)
    private String otp;
}
