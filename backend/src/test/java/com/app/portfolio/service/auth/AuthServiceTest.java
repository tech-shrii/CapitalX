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
import com.app.portfolio.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private OtpRequest otpRequest;
    private User testUser;
    private OtpToken testOtpToken;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("123456");

        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .enabled(true)  // User must be enabled for login
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testOtpToken = OtpToken.builder()
                .id(1L)
                .user(testUser)
                .token("123456")
                .expiresAt(Instant.now().plusSeconds(300))
                .purpose("SIGNUP")
                .used(false)
                .build();
    }

    @Nested
    @DisplayName("Signup Tests")
    class SignupTests {

        @Test
        @DisplayName("Should create user and send OTP when signup request is valid")
        void shouldCreateUserAndSendOtpWhenSignupRequestIsValid() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(testOtpToken);

            authService.signup(signupRequest);

            verify(userRepository).existsByEmail("test@example.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
            verify(otpTokenRepository).save(any(OtpToken.class));
            verify(emailService).sendOtpEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when passwords do not match")
        void shouldThrowExceptionWhenPasswordsDoNotMatch() {
            signupRequest.setConfirmPassword("different");

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Passwords do not match");

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
            verify(otpTokenRepository, never()).save(any(OtpToken.class));
            verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email already registered");

            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).save(any(User.class));
            verify(otpTokenRepository, never()).save(any(OtpToken.class));
            verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Verify Signup OTP Tests")
    class VerifySignupOtpTests {

        @Test
        @DisplayName("Should verify OTP and enable user when OTP is valid")
        void shouldVerifyOtpAndEnableUserWhenOtpIsValid() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    anyLong(), anyString(), anyString(), any(Instant.class)))
                    .thenReturn(Optional.of(testOtpToken));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

            AuthResponse result = authService.verifySignupOtp(otpRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getName()).isEqualTo("Test User");

            verify(userRepository).findByEmail("test@example.com");
            verify(otpTokenRepository).findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    anyLong(), anyString(), anyString(), any(Instant.class));
            verify(otpTokenRepository).save(testOtpToken);
            verify(userRepository).save(any(User.class));
            verify(tokenProvider).generateToken(any(Authentication.class));
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when email does not exist")
        void shouldThrowExceptionWhenEmailDoesNotExist() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifySignupOtp(otpRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email");

            verify(userRepository).findByEmail("test@example.com");
            verify(otpTokenRepository, never()).save(any(OtpToken.class));
            verify(userRepository, never()).save(any(User.class));
            verify(tokenProvider, never()).generateToken(any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should send OTP when login credentials are valid")
        void shouldSendOtpWhenLoginCredentialsAreValid() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(otpTokenRepository.save(any(OtpToken.class))).thenReturn(testOtpToken);

            authService.login(loginRequest);

            verify(userRepository).findByEmail("test@example.com");
            verify(passwordEncoder).matches("password123", "encodedPassword");
            verify(otpTokenRepository).save(any(OtpToken.class));
            verify(emailService).sendOtpEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication fails")
        void shouldThrowExceptionWhenAuthenticationFails() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");

            verify(userRepository).findByEmail("test@example.com");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(otpTokenRepository, never()).save(any(OtpToken.class));
            verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Verify Login OTP Tests")
    class VerifyLoginOtpTests {

        @Test
        @DisplayName("Should verify OTP and return auth response when OTP is valid")
        void shouldVerifyOtpAndReturnAuthResponseWhenOtpIsValid() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    anyLong(), anyString(), anyString(), any(Instant.class)))
                    .thenReturn(Optional.of(testOtpToken));
            when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("jwt-token");

            AuthResponse result = authService.verifyLoginOtp(otpRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getType()).isEqualTo("Bearer");
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getName()).isEqualTo("Test User");

            verify(userRepository).findByEmail("test@example.com");
            verify(otpTokenRepository).findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    anyLong(), anyString(), anyString(), any(Instant.class));
            verify(otpTokenRepository).save(testOtpToken);
            verify(tokenProvider).generateToken(any(Authentication.class));
        }
    }
}
