package com.app.portfolio.controller;

import com.app.portfolio.dto.auth.AuthResponse;
import com.app.portfolio.dto.auth.LoginRequest;
import com.app.portfolio.dto.auth.OtpRequest;
import com.app.portfolio.dto.auth.SignupRequest;
import com.app.portfolio.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok().body(java.util.Map.of("message", "OTP sent to your email"));
    }

    @PostMapping("/verify-signup-otp")
    public ResponseEntity<AuthResponse> verifySignupOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.verifySignupOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        authService.login(request);
        return ResponseEntity.ok().body(java.util.Map.of("message", "OTP sent to your email"));
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<AuthResponse> verifyLoginOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }
}
