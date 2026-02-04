// File: backend/src/test/java/com/app/portfolio/repository/UserRepositoryTest.java
package com.app.portfolio.repository;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Repository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .password("password123")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUser2 = User.builder()
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .password("password456")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUser3 = User.builder()
                .name("Bob Johnson")
                .email("bob.johnson@example.com")
                .password("password789")
                .enabled(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.persist(testUser3);
        entityManager.flush();
    }

    @Nested
    @DisplayName("FindByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email when exists")
        void shouldFindUserByEmailWhenExists() {
            Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");

            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getName()).isEqualTo("John Doe");
            assertThat(foundUser.get().getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Should return empty when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should be case insensitive when finding by email")
        void shouldBeCaseInsensitiveWhenFindingByEmail() {
            Optional<User> foundUser = userRepository.findByEmail("JOHN.DOE@EXAMPLE.COM");

            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should find disabled user by email")
        void shouldFindDisabledUserByEmail() {
            Optional<User> foundUser = userRepository.findByEmail("bob.johnson@example.com");

            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getName()).isEqualTo("Bob Johnson");
            assertThat(foundUser.get().getEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ExistsByEmail Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when user exists with email")
        void shouldReturnTrueWhenUserExistsWithEmail() {
            boolean exists = userRepository.existsByEmail("jane.smith@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when user does not exist with email")
        void shouldReturnFalseWhenUserDoesNotExistWithEmail() {
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true for disabled user email")
        void shouldReturnTrueForDisabledUserEmail() {
            boolean exists = userRepository.existsByEmail("bob.johnson@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should be case sensitive for email existence check")
        void shouldBeCaseSensitiveForEmailExistenceCheck() {
            boolean exists = userRepository.existsByEmail("JOHN.DOE@EXAMPLE.COM");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Standard JpaRepository Tests")
    class StandardJpaRepositoryTests {

        @Test
        @DisplayName("Should save user successfully")
        void shouldSaveUserSuccessfully() {
            User newUser = User.builder()
                    .name("Alice Wilson")
                    .email("alice.wilson@example.com")
                    .password("newpassword")
                    .enabled(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User savedUser = userRepository.save(newUser);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("Alice Wilson");
            assertThat(savedUser.getEmail()).isEqualTo("alice.wilson@example.com");
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            Optional<User> foundUser = userRepository.findById(testUser1.getId());

            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should return empty when user not found by ID")
        void shouldReturnEmptyWhenUserNotFoundById() {
            Optional<User> foundUser = userRepository.findById(999L);

            assertThat(foundUser).isEmpty();
        }

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            userRepository.delete(testUser1);
            entityManager.flush();

            Optional<User> deletedUser = userRepository.findById(testUser1.getId());

            assertThat(deletedUser).isEmpty();
        }

        @Test
        @DisplayName("Should count all users")
        void shouldCountAllUsers() {
            long count = userRepository.count();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            List<User> allUsers = userRepository.findAll();

            assertThat(allUsers).hasSize(3);
            assertThat(allUsers).extracting(User::getName)
                    .containsExactlyInAnyOrder("John Doe", "Jane Smith", "Bob Johnson");
        }

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            testUser1.setName("Updated John Doe");
            testUser1.setEnabled(false);

            User updatedUser = userRepository.save(testUser1);

            assertThat(updatedUser.getName()).isEqualTo("Updated John Doe");
            assertThat(updatedUser.getEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Email Uniqueness Tests")
    class EmailUniquenessTests {

        @Test
        @DisplayName("Should enforce email uniqueness constraint")
        void shouldEnforceEmailUniquenessConstraint() {
            User duplicateUser = User.builder()
                    .name("Duplicate User")
                    .email("john.doe@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                entityManager.persist(duplicateUser);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("Should allow different emails for different users")
        void shouldAllowDifferentEmailsForDifferentUsers() {
            User newUser = User.builder()
                    .name("New User")
                    .email("new.user@example.com")
                    .password("password")
                    .enabled(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User savedUser = entityManager.persistAndFlush(newUser);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo("new.user@example.com");
        }
    }

    @Nested
    @DisplayName("User Status Tests")
    class UserStatusTests {

        @Test
        @DisplayName("Should save user with enabled status true by default")
        void shouldSaveUserWithEnabledStatusTrueByDefault() {
            User newUser = User.builder()
                    .name("Default User")
                    .email("default@example.com")
                    .password("password")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User savedUser = userRepository.save(newUser);

            assertThat(savedUser.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should save user with explicit enabled status false")
        void shouldSaveUserWithExplicitEnabledStatusFalse() {
            User disabledUser = User.builder()
                    .name("Disabled User")
                    .email("disabled@example.com")
                    .password("password")
                    .enabled(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User savedUser = userRepository.save(disabledUser);

            assertThat(savedUser.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should find both enabled and disabled users")
        void shouldFindBothEnabledAndDisabledUsers() {
            List<User> allUsers = userRepository.findAll();

            assertThat(allUsers).hasSize(3);
            assertThat(allUsers).extracting(User::getEnabled)
                    .containsExactlyInAnyOrder(true, true, false);
        }
    }
}
