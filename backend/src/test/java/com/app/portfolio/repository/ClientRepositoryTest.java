// File: backend/src/test/java/com/app/portfolio/repository/ClientRepositoryTest.java
package com.app.portfolio.repository;

import com.app.portfolio.beans.Client;
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
@DisplayName("Client Repository Tests")
class ClientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    private User testUser;
    private Client testClient1;
    private Client testClient2;
    private Client testClient3;
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

        testClient1 = Client.builder()
                .user(testUser)
                .name("Client One")
                .email("client1@example.com")
                .phone("1234567890")
                .currency("USD")
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        testClient2 = Client.builder()
                .user(testUser)
                .name("Client Two")
                .email("client2@example.com")
                .phone("0987654321")
                .currency("EUR")
                .createdAt(Instant.now().minusSeconds(1800))
                .updatedAt(Instant.now().minusSeconds(1800))
                .build();

        testClient3 = Client.builder()
                .user(testUser)
                .name("Client Three")
                .email("client3@example.com")
                .phone("5555555555")
                .currency("GBP")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        entityManager.persist(testUser);
        entityManager.persist(otherUser);
        entityManager.persist(testClient1);
        entityManager.persist(testClient2);
        entityManager.persist(testClient3);
        entityManager.flush();
    }

    @Nested
    @DisplayName("FindByUserIdOrderByCreatedAtDesc Tests")
    class FindByUserIdOrderByCreatedAtDescTests {

        @Test
        @DisplayName("Should return clients ordered by created at descending")
        void shouldReturnClientsOrderedByCreatedAtDesc() {
            List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertThat(clients).hasSize(3);
            assertThat(clients.get(0).getName()).isEqualTo("Client Three");
            assertThat(clients.get(1).getName()).isEqualTo("Client Two");
            assertThat(clients.get(2).getName()).isEqualTo("Client One");
        }

        @Test
        @DisplayName("Should return empty list for non-existent user ID")
        void shouldReturnEmptyListForNonExistentUserId() {
            List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(999L);

            assertThat(clients).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when user has no clients")
        void shouldReturnEmptyListWhenUserHasNoClients() {
            List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(otherUser.getId());

            assertThat(clients).isEmpty();
        }

        @Test
        @DisplayName("Should not return clients belonging to other users")
        void shouldNotReturnClientsBelongingToOtherUsers() {
            Client otherClient = Client.builder()
                    .user(otherUser)
                    .name("Other Client")
                    .email("otherclient@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            entityManager.persist(otherClient);
            entityManager.flush();

            List<Client> clients = clientRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertThat(clients).hasSize(3);
            assertThat(clients).extracting(Client::getName)
                    .containsExactly("Client Three", "Client Two", "Client One");
            assertThat(clients).extracting(Client::getName)
                    .doesNotContain("Other Client");
        }
    }

    @Nested
    @DisplayName("ExistsByIdAndUserId Tests")
    class ExistsByIdAndUserIdTests {

        @Test
        @DisplayName("Should return true when client exists and belongs to user")
        void shouldReturnTrueWhenClientExistsAndBelongsToUser() {
            boolean exists = clientRepository.existsByIdAndUserId(testClient1.getId(), testUser.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when client exists but belongs to different user")
        void shouldReturnFalseWhenClientExistsButBelongsToDifferentUser() {
            boolean exists = clientRepository.existsByIdAndUserId(testClient1.getId(), otherUser.getId());

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when client does not exist")
        void shouldReturnFalseWhenClientDoesNotExist() {
            boolean exists = clientRepository.existsByIdAndUserId(999L, testUser.getId());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Standard JpaRepository Tests")
    class StandardJpaRepositoryTests {

        @Test
        @DisplayName("Should save client successfully")
        void shouldSaveClientSuccessfully() {
            Client newClient = Client.builder()
                    .user(testUser)
                    .name("New Client")
                    .email("newclient@example.com")
                    .phone("1111111111")
                    .currency("JPY")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Client savedClient = clientRepository.save(newClient);

            assertThat(savedClient.getId()).isNotNull();
            assertThat(savedClient.getName()).isEqualTo("New Client");
            assertThat(savedClient.getEmail()).isEqualTo("newclient@example.com");
        }

        @Test
        @DisplayName("Should find client by ID")
        void shouldFindClientById() {
            Optional<Client> foundClient = clientRepository.findById(testClient1.getId());

            assertThat(foundClient).isPresent();
            assertThat(foundClient.get().getName()).isEqualTo("Client One");
        }

        @Test
        @DisplayName("Should return empty when client not found by ID")
        void shouldReturnEmptyWhenClientNotFoundById() {
            Optional<Client> foundClient = clientRepository.findById(999L);

            assertThat(foundClient).isEmpty();
        }

        @Test
        @DisplayName("Should delete client successfully")
        void shouldDeleteClientSuccessfully() {
            clientRepository.delete(testClient1);
            entityManager.flush();

            Optional<Client> deletedClient = clientRepository.findById(testClient1.getId());

            assertThat(deletedClient).isEmpty();
        }

        @Test
        @DisplayName("Should count all clients")
        void shouldCountAllClients() {
            long count = clientRepository.count();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all clients")
        void shouldFindAllClients() {
            List<Client> allClients = clientRepository.findAll();

            assertThat(allClients).hasSize(3);
            assertThat(allClients).extracting(Client::getName)
                    .containsExactlyInAnyOrder("Client One", "Client Two", "Client Three");
        }

        @Test
        @DisplayName("Should update client successfully")
        void shouldUpdateClientSuccessfully() {
            testClient1.setName("Updated Client Name");
            testClient1.setPhone("9999999999");

            Client updatedClient = clientRepository.save(testClient1);

            assertThat(updatedClient.getName()).isEqualTo("Updated Client Name");
            assertThat(updatedClient.getPhone()).isEqualTo("9999999999");
        }
    }

    @Nested
    @DisplayName("Entity Relationship Tests")
    class EntityRelationshipTests {

        @Test
        @DisplayName("Should maintain relationship with user")
        void shouldMaintainRelationshipWithUser() {
            Optional<Client> foundClient = clientRepository.findById(testClient1.getId());

            assertThat(foundClient).isPresent();
            assertThat(foundClient.get().getUser().getId()).isEqualTo(testUser.getId());
            assertThat(foundClient.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
        }

        @Test
        @DisplayName("Should handle cascade operations correctly")
        void shouldHandleCascadeOperationsCorrectly() {
            Client newClient = Client.builder()
                    .user(testUser)
                    .name("Cascade Test Client")
                    .email("cascade@example.com")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Client savedClient = clientRepository.save(newClient);

            entityManager.flush();
            entityManager.clear();

            Optional<Client> retrievedClient = clientRepository.findById(savedClient.getId());

            assertThat(retrievedClient).isPresent();
            assertThat(retrievedClient.get().getUser()).isNotNull();
            assertThat(retrievedClient.get().getUser().getId()).isEqualTo(testUser.getId());
        }
    }
}
