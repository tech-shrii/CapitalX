package com.app.portfolio.controller;

import com.app.portfolio.dto.profile.ProfileRequest;
import com.app.portfolio.dto.profile.ProfileResponse;
import com.app.portfolio.dto.profile.ResetPasswordRequest;
import com.app.portfolio.exceptions.BadRequestException;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .map(user -> ResponseEntity.ok(ProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileRequest request,
                                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .map(user -> {
                    if (request.getName() != null) {
                        user.setName(request.getName());
                    }
                    user = userRepository.save(user);
                    return ResponseEntity.ok(ProfileResponse.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .map(user -> {
                    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new BadRequestException("Current password is incorrect");
                    }
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);
                    return ResponseEntity.ok().body(java.util.Map.of("message", "Password reset successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
