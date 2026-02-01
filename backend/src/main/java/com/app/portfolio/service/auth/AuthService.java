package com.app.portfolio.service.auth;

import com.app.portfolio.dto.auth.AuthResponse;
import com.app.portfolio.dto.auth.LoginRequest;
import com.app.portfolio.dto.auth.OtpRequest;
import com.app.portfolio.dto.auth.SignupRequest;

public interface AuthService {

    void signup(SignupRequest request);

    AuthResponse verifySignupOtp(OtpRequest request);

    void login(LoginRequest request);

    AuthResponse verifyLoginOtp(OtpRequest request);
}
