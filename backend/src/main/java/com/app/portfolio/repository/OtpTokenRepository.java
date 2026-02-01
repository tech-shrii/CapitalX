package com.app.portfolio.repository;

import com.app.portfolio.beans.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
            Long userId, String token, String purpose, Instant now);

    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(Instant instant);
}
