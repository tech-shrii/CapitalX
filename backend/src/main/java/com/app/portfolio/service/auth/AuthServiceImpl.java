package com.app.portfolio.service.auth;

import com.app.portfolio.beans.OtpToken;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.auth.AuthResponse;
import com.app.portfolio.dto.auth.LoginRequest;
import com.app.portfolio.dto.auth.OtpRequest;
import com.app.portfolio.dto.auth.SignupRequest;
import com.app.portfolio.exceptions.BadRequestException;
import com.app.portfolio.exceptions.UnauthorizedException;
import com.app.portfolio.repository.OtpTokenRepository;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.security.JwtTokenProvider;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final Random random = new Random();

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(600); // 10 minutes

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false) // Will be enabled after OTP verification
                .build();

        user = userRepository.save(user);

        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .token(otp)
                .expiresAt(expiresAt)
                .purpose("SIGNUP")
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(user.getEmail(), otp, "Signup");
    }

    @Override
    @Transactional
    public AuthResponse verifySignupOtp(OtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));

        OtpToken otpToken = otpTokenRepository
                .findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                        user.getId(), request.getOtp(), "SIGNUP", Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        user.setEnabled(true);
        user = userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create(user), null, UserPrincipal.create(user).getAuthorities());
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Override
    @Transactional
    public void login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account not verified. Please verify your email.");
        }

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plusSeconds(600); // 10 minutes

        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .token(otp)
                .expiresAt(expiresAt)
                .purpose("LOGIN")
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(user.getEmail(), otp, "Login");
    }

    @Override
    @Transactional
    public AuthResponse verifyLoginOtp(OtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));

        OtpToken otpToken = otpTokenRepository
                .findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                        user.getId(), request.getOtp(), "LOGIN", Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create(user), null, UserPrincipal.create(user).getAuthorities());
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    private String generateOtp() {
        return String.format("%06d", random.nextInt(1000000));
    }
}
