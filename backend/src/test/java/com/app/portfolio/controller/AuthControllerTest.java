package com.app.portfolio.controller;

import com.app.portfolio.dto.auth.*;
import com.app.portfolio.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @org.junit.jupiter.api.Test
    @DisplayName("Signup returns success message when request is valid")
    void signup_ReturnsSuccessMessage_WhenRequestIsValid() {
        SignupRequest request = mock(SignupRequest.class);
        doNothing().when(authService).signup(request);

        ResponseEntity<?> response = authController.signup(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(((java.util.Map<?, ?>) response.getBody()).get("message")).isEqualTo("OTP sent to your email");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Signup throws exception when service throws exception")
    void signup_ThrowsException_WhenServiceThrowsException() {
        SignupRequest request = mock(SignupRequest.class);
        doThrow(new RuntimeException("error")).when(authService).signup(request);

        assertThrows(RuntimeException.class, () -> authController.signup(request));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Verify signup OTP returns AuthResponse when OTP is valid")
    void verifySignupOtp_ReturnsAuthResponse_WhenOtpIsValid() {
        OtpRequest request = mock(OtpRequest.class);
        AuthResponse authResponse = mock(AuthResponse.class);
        when(authService.verifySignupOtp(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.verifySignupOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Verify signup OTP throws exception when service throws exception")
    void verifySignupOtp_ThrowsException_WhenServiceThrowsException() {
        OtpRequest request = mock(OtpRequest.class);
        when(authService.verifySignupOtp(request)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> authController.verifySignupOtp(request));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Login returns success message when request is valid")
    void login_ReturnsSuccessMessage_WhenRequestIsValid() {
        LoginRequest request = mock(LoginRequest.class);
        doNothing().when(authService).login(request);

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(((java.util.Map<?, ?>) response.getBody()).get("message")).isEqualTo("OTP sent to your email");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Login throws exception when service throws exception")
    void login_ThrowsException_WhenServiceThrowsException() {
        LoginRequest request = mock(LoginRequest.class);
        doThrow(new RuntimeException("error")).when(authService).login(request);

        assertThrows(RuntimeException.class, () -> authController.login(request));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Verify login OTP returns AuthResponse when OTP is valid")
    void verifyLoginOtp_ReturnsAuthResponse_WhenOtpIsValid() {
        OtpRequest request = mock(OtpRequest.class);
        AuthResponse authResponse = mock(AuthResponse.class);
        when(authService.verifyLoginOtp(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.verifyLoginOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Verify login OTP throws exception when service throws exception")
    void verifyLoginOtp_ThrowsException_WhenServiceThrowsException() {
        OtpRequest request = mock(OtpRequest.class);
        when(authService.verifyLoginOtp(request)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> authController.verifyLoginOtp(request));
    }
}
