// File: backend/src/test/java/com/app/portfolio/repository/OtpTokenRepositoryTest.java
package com.app.portfolio.repository;

import com.app.portfolio.beans.OtpToken;
import com.app.portfolio.beans.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("OTP Token Repository Tests")
class OtpTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    private User testUser;
    private OtpToken validToken;
    private OtpToken expiredToken;
    private OtpToken usedToken;
    private OtpToken otherPurposeToken;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        otherUser = User.builder()
                .name("Other User")
                .email("other@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant oneHourFromNow = now.plus(1, ChronoUnit.HOURS);

        validToken = OtpToken.builder()
                .user(testUser)
                .token("123456")
                .expiresAt(oneHourFromNow)
                .used(false)
                .purpose("SIGNUP")
                .build();

        expiredToken = OtpToken.builder()
                .user(testUser)
                .token("654321")
                .expiresAt(oneHourAgo)
                .used(false)
                .purpose("LOGIN")
                .build();

        usedToken = OtpToken.builder()
                .user(testUser)
                .token("111111")
                .expiresAt(oneHourFromNow)
                .used(true)
                .purpose("PASSWORD_RESET")
                .build();

        otherPurposeToken = OtpToken.builder()
                .user(testUser)
                .token("222222")
                .expiresAt(oneHourFromNow)
                .used(false)
                .purpose("OTHER")
                .build();

        entityManager.persist(testUser);
        entityManager.persist(otherUser);
        entityManager.persist(validToken);
        entityManager.persist(expiredToken);
        entityManager.persist(usedToken);
        entityManager.persist(otherPurposeToken);
        entityManager.flush();
    }

    @Nested
    @DisplayName("FindByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter Tests")
    class FindByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfterTests {

        @Test
        @DisplayName("Should find valid token matching all criteria")
        void shouldFindValidTokenMatchingAllCriteria() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "123456", "SIGNUP", Instant.now()
            );

            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getToken()).isEqualTo("123456");
            assertThat(foundToken.get().getPurpose()).isEqualTo("SIGNUP");
            assertThat(foundToken.get().getUsed()).isFalse();
        }

        @Test
        @DisplayName("Should not find token with wrong user ID")
        void shouldNotFindTokenWithWrongUserId() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    otherUser.getId(), "123456", "SIGNUP", Instant.now()
            );

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should not find token with wrong token value")
        void shouldNotFindTokenWithWrongTokenValue() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "999999", "SIGNUP", Instant.now()
            );

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should not find token with wrong purpose")
        void shouldNotFindTokenWithWrongPurpose() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "123456", "LOGIN", Instant.now()
            );

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should not find used token")
        void shouldNotFindUsedToken() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "111111", "PASSWORD_RESET", Instant.now()
            );

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should not find expired token")
        void shouldNotFindExpiredToken() {
            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "654321", "LOGIN", Instant.now()
            );

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should find token when checked before expiration time")
        void shouldFindTokenWhenCheckedBeforeExpirationTime() {
            Instant beforeExpiration = Instant.now().plus(30, ChronoUnit.MINUTES);

            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "123456", "SIGNUP", beforeExpiration
            );

            assertThat(foundToken).isPresent();
        }

        @Test
        @DisplayName("Should not find token when checked after expiration time")
        void shouldNotFindTokenWhenCheckedAfterExpirationTime() {
            Instant afterExpiration = Instant.now().plus(2, ChronoUnit.HOURS);

            Optional<OtpToken> foundToken = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "123456", "SIGNUP", afterExpiration
            );

            assertThat(foundToken).isEmpty();
        }
    }

    @Nested
    @DisplayName("DeleteByUserId Tests")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("Should delete all tokens for user")
        void shouldDeleteAllTokensForUser() {
            assertThat(otpTokenRepository.count()).isEqualTo(4);

            otpTokenRepository.deleteByUserId(testUser.getId());
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not affect tokens for other users")
        void shouldNotAffectTokensForOtherUsers() {
            OtpToken otherUserToken = OtpToken.builder()
                    .user(otherUser)
                    .token("333333")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("SIGNUP")
                    .build();

            entityManager.persist(otherUserToken);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(5);

            otpTokenRepository.deleteByUserId(testUser.getId());
            entityManager.flush();

            List<OtpToken> remainingTokens = otpTokenRepository.findAll();
            assertThat(remainingTokens).hasSize(1);
            assertThat(remainingTokens.get(0).getUser().getId()).isEqualTo(otherUser.getId());
        }

        @Test
        @DisplayName("Should handle non-existent user ID gracefully")
        void shouldHandleNonExistentUserIdGracefully() {
            long initialCount = otpTokenRepository.count();

            otpTokenRepository.deleteByUserId(999L);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(initialCount);
        }
    }

    @Nested
    @DisplayName("DeleteByExpiresAtBefore Tests")
    class DeleteByExpiresAtBeforeTests {

        @Test
        @DisplayName("Should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            Instant cutoffTime = Instant.now();

            assertThat(otpTokenRepository.count()).isEqualTo(4);

            otpTokenRepository.deleteByExpiresAtBefore(cutoffTime);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(3);

            List<OtpToken> remainingTokens = otpTokenRepository.findAll();
            assertThat(remainingTokens).extracting(OtpToken::getToken)
                    .containsExactlyInAnyOrder("123456", "111111", "222222");
        }

        @Test
        @DisplayName("Should not delete non-expired tokens")
        void shouldNotDeleteNonExpiredTokens() {
            Instant pastTime = Instant.now().minus(2, ChronoUnit.HOURS);

            assertThat(otpTokenRepository.count()).isEqualTo(4);

            otpTokenRepository.deleteByExpiresAtBefore(pastTime);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should delete tokens when cutoff time is after their expiration")
        void shouldDeleteTokensWhenCutoffTimeIsAfterTheirExpiration() {
            Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);

            assertThat(otpTokenRepository.count()).isEqualTo(4);

            otpTokenRepository.deleteByExpiresAtBefore(futureTime);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should delete all tokens when cutoff is far in future")
        void shouldDeleteAllTokensWhenCutoffIsFarInFuture() {
            Instant farFuture = Instant.now().plus(10, ChronoUnit.DAYS);

            assertThat(otpTokenRepository.count()).isEqualTo(4);

            otpTokenRepository.deleteByExpiresAtBefore(farFuture);
            entityManager.flush();

            assertThat(otpTokenRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Standard JpaRepository Tests")
    class StandardJpaRepositoryTests {

        @Test
        @DisplayName("Should save OTP token successfully")
        void shouldSaveOtpTokenSuccessfully() {
            OtpToken newToken = OtpToken.builder()
                    .user(testUser)
                    .token("444444")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("SIGNUP")
                    .build();

            OtpToken savedToken = otpTokenRepository.save(newToken);

            assertThat(savedToken.getId()).isNotNull();
            assertThat(savedToken.getToken()).isEqualTo("444444");
            assertThat(savedToken.getPurpose()).isEqualTo("SIGNUP");
        }

        @Test
        @DisplayName("Should find OTP token by ID")
        void shouldFindOtpTokenById() {
            Optional<OtpToken> foundToken = otpTokenRepository.findById(validToken.getId());

            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getToken()).isEqualTo("123456");
        }

        @Test
        @DisplayName("Should return empty when OTP token not found by ID")
        void shouldReturnEmptyWhenOtpTokenNotFoundById() {
            Optional<OtpToken> foundToken = otpTokenRepository.findById(999L);

            assertThat(foundToken).isEmpty();
        }

        @Test
        @DisplayName("Should delete OTP token successfully")
        void shouldDeleteOtpTokenSuccessfully() {
            otpTokenRepository.delete(validToken);
            entityManager.flush();

            Optional<OtpToken> deletedToken = otpTokenRepository.findById(validToken.getId());

            assertThat(deletedToken).isEmpty();
        }

        @Test
        @DisplayName("Should count all OTP tokens")
        void shouldCountAllOtpTokens() {
            long count = otpTokenRepository.count();

            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("Should find all OTP tokens")
        void shouldFindAllOtpTokens() {
            List<OtpToken> allTokens = otpTokenRepository.findAll();

            assertThat(allTokens).hasSize(4);
            assertThat(allTokens).extracting(OtpToken::getToken)
                    .containsExactlyInAnyOrder("123456", "654321", "111111", "222222");
        }

        @Test
        @DisplayName("Should update OTP token successfully")
        void shouldUpdateOtpTokenSuccessfully() {
            validToken.setUsed(true);
            validToken.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));

            OtpToken updatedToken = otpTokenRepository.save(validToken);

            assertThat(updatedToken.getUsed()).isTrue();
            assertThat(updatedToken.getExpiresAt()).isAfter(validToken.getExpiresAt().minusSeconds(1));
        }
    }

    @Nested
    @DisplayName("Token Purpose Tests")
    class TokenPurposeTests {

        @Test
        @DisplayName("Should handle all standard purposes")
        void shouldHandleAllStandardPurposes() {
            OtpToken signupToken = OtpToken.builder()
                    .user(testUser)
                    .token("555555")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("SIGNUP")
                    .build();

            OtpToken loginToken = OtpToken.builder()
                    .user(testUser)
                    .token("666666")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("LOGIN")
                    .build();

            OtpToken passwordResetToken = OtpToken.builder()
                    .user(testUser)
                    .token("777777")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("PASSWORD_RESET")
                    .build();

            entityManager.persist(signupToken);
            entityManager.persist(loginToken);
            entityManager.persist(passwordResetToken);
            entityManager.flush();

            Optional<OtpToken> foundSignup = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "555555", "SIGNUP", Instant.now()
            );
            Optional<OtpToken> foundLogin = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "666666", "LOGIN", Instant.now()
            );
            Optional<OtpToken> foundPasswordReset = otpTokenRepository.findByUserIdAndTokenAndPurposeAndUsedFalseAndExpiresAtAfter(
                    testUser.getId(), "777777", "PASSWORD_RESET", Instant.now()
            );

            assertThat(foundSignup).isPresent();
            assertThat(foundLogin).isPresent();
            assertThat(foundPasswordReset).isPresent();
        }
    }

    @Nested
    @DisplayName("Entity Relationship Tests")
    class EntityRelationshipTests {

        @Test
        @DisplayName("Should maintain relationship with user")
        void shouldMaintainRelationshipWithUser() {
            Optional<OtpToken> foundToken = otpTokenRepository.findById(validToken.getId());

            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getUser().getId()).isEqualTo(testUser.getId());
            assertThat(foundToken.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
        }

        @Test
        @DisplayName("Should handle cascade operations correctly")
        void shouldHandleCascadeOperationsCorrectly() {
            OtpToken newToken = OtpToken.builder()
                    .user(testUser)
                    .token("888888")
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .used(false)
                    .purpose("SIGNUP")
                    .build();

            OtpToken savedToken = otpTokenRepository.save(newToken);

            entityManager.flush();
            entityManager.clear();

            Optional<OtpToken> retrievedToken = otpTokenRepository.findById(savedToken.getId());

            assertThat(retrievedToken).isPresent();
            assertThat(retrievedToken.get().getUser()).isNotNull();
            assertThat(retrievedToken.get().getUser().getId()).isEqualTo(testUser.getId());
        }
    }
}
