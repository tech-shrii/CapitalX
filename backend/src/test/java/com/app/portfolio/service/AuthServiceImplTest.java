package com.app.portfolio.service;

import com.app.portfolio.beans.*;
import com.app.portfolio.dto.auth.*;
import com.app.portfolio.exceptions.*;
import com.app.portfolio.repository.*;
import com.app.portfolio.security.*;
import com.app.portfolio.service.auth.AuthServiceImpl;
import com.app.portfolio.service.email.EmailService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpTokenRepository otpTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_shouldSendOtp_whenValidRequest() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("pass")
                .confirmPassword("pass")
                .name("Test").build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(otpTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.signup(request);

        verify(emailService).sendOtpEmail(eq("test@example.com"), any(), eq("Signup"));
    }

    @Test
    void signup_shouldThrow_whenPasswordsDoNotMatch() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("pass")
                .confirmPassword("fail")
                .name("Test").build();

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void signup_shouldThrow_whenEmailExists() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("pass")
                .confirmPassword("pass")
                .name("Test").build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void verifySignupOtp_shouldEnableUserAndReturnToken_whenValidOtp() {
        User user = User.builder().id(1L).email("test@example.com").enabled(false).build();
        OtpToken otpToken = OtpToken.builder().user(user).token("123456").purpose("SIGNUP").expiresAt(Instant.now().plusSeconds(100)).used(false).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(eq(1L), eq("123456"), eq("SIGNUP"), any()))
                .thenReturn(Optional.of(otpToken));
        when(userRepository.save(any())).thenReturn(user);
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token");

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("123456");

        AuthResponse response = authService.verifySignupOtp(otpRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void verifySignupOtp_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("123456");

        assertThatThrownBy(() -> authService.verifySignupOtp(otpRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifySignupOtp_shouldThrow_whenOtpInvalid() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("bad");

        assertThatThrownBy(() -> authService.verifySignupOtp(otpRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_shouldSendOtp_whenValidCredentials() {
        User user = User.builder().id(1L).email("test@example.com").password("encoded").enabled(true).build();
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
        when(otpTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.login(request);

        verify(emailService).sendOtpEmail(eq("test@example.com"), any(), eq("Login"));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_shouldThrow_whenPasswordIncorrect() {
        User user = User.builder().id(1L).email("test@example.com").password("encoded").enabled(true).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_shouldThrow_whenUserNotEnabled() {
        User user = User.builder().id(1L).email("test@example.com").password("encoded").enabled(false).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verifyLoginOtp_shouldReturnToken_whenValidOtp() {
        User user = User.builder().id(1L).email("test@example.com").enabled(true).build();
        OtpToken otpToken = OtpToken.builder().user(user).token("654321").purpose("LOGIN").expiresAt(Instant.now().plusSeconds(100)).used(false).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(eq(1L), eq("654321"), eq("LOGIN"), any()))
                .thenReturn(Optional.of(otpToken));
        when(tokenProvider.generateToken(any())).thenReturn("jwt-login-token");

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("654321");

        AuthResponse response = authService.verifyLoginOtp(otpRequest);

        assertThat(response.getToken()).isEqualTo("jwt-login-token");
    }

    @Test
    void verifyLoginOtp_shouldThrow_whenOtpInvalid() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail("test@example.com");
        otpRequest.setOtp("bad");

        assertThatThrownBy(() -> authService.verifyLoginOtp(otpRequest))
                .isInstanceOf(UnauthorizedException.class);
    }
}
