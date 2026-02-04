package com.app.portfolio.controller;

import com.app.portfolio.dto.auth.AuthResponse;
import com.app.portfolio.dto.auth.LoginRequest;
import com.app.portfolio.dto.auth.OtpRequest;
import com.app.portfolio.dto.auth.SignupRequest;
import com.app.portfolio.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signup_shouldReturnSuccessMessage() throws Exception {
        SignupRequest signupRequest = new SignupRequest("Test User", "test@example.com", "password");
        doNothing().when(authService).signup(any(SignupRequest.class));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your email"));
    }

    @Test
    void verifySignupOtp_shouldReturnAuthResponse() throws Exception {
        OtpRequest otpRequest = new OtpRequest("test@example.com", "123456");
        AuthResponse authResponse = new AuthResponse("test-token", "Test User", "test@example.com");

        when(authService.verifySignupOtp(any(OtpRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/verify-signup-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_shouldReturnSuccessMessage() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password");
        doNothing().when(authService).login(any(LoginRequest.class));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your email"));
    }

    @Test
    void verifyLoginOtp_shouldReturnAuthResponse() throws Exception {
        OtpRequest otpRequest = new OtpRequest();
        AuthResponse authResponse = new AuthResponse("test-token", "Test User", "test@example.com");

        when(authService.verifyLoginOtp(any(OtpRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/verify-login-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
