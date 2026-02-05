package com.app.portfolio.service.client;

import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.ClientMapper;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client Service Tests")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    private User testUser;
    private Client testClient1;
    private Client testClient2;
    private ClientRequest clientRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testClient1 = Client.builder()
                .id(1L)
                .name("Client 1")
                .email("client1@example.com")
                .user(testUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testClient2 = Client.builder()
                .id(2L)
                .name("Client 2")
                .email("client2@example.com")
                .user(testUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        clientRequest = new ClientRequest();
        clientRequest.setName("New Client");
        clientRequest.setEmail("newclient@example.com");
    }

    @Nested
    @DisplayName("Create Client Tests")
    class CreateClientTests {

        @Test
        @DisplayName("Should create client when user exists")
        void shouldCreateClientWhenUserExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(clientMapper.toEntity(clientRequest, null)).thenReturn(testClient1);
            when(clientRepository.save(any(Client.class))).thenReturn(testClient1);
            when(clientMapper.toResponse(testClient1)).thenReturn(ClientResponse.builder()
                    .id(1L)
                    .name("Client 1")
                    .email("client1@example.com")
                    .build());

            ClientResponse result = clientService.createClient(clientRequest, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Client 1");
            assertThat(result.getEmail()).isEqualTo("client1@example.com");

            verify(userRepository).findById(1L);
            verify(clientMapper).toEntity(clientRequest, null);
            verify(clientRepository).save(any(Client.class));
            verify(clientMapper).toResponse(testClient1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.createClient(clientRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found with id: 1");

            verify(userRepository).findById(1L);
            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("Get Client Tests")
    class GetClientTests {

        @Test
        @DisplayName("Should return client when it exists and belongs to user")
        void shouldReturnClientWhenItExistsAndBelongsToUser() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient1));
            when(clientMapper.toResponse(testClient1)).thenReturn(ClientResponse.builder()
                    .id(1L)
                    .name("Client 1")
                    .email("client1@example.com")
                    .build());

            ClientResponse result = clientService.getClientById(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Client 1");
            assertThat(result.getEmail()).isEqualTo("client1@example.com");

            verify(clientRepository).findById(1L);
            verify(clientMapper).toResponse(testClient1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientDoesNotExist() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.getClientById(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not belong to user")
        void shouldThrowExceptionWhenClientDoesNotBelongToUser() {
            User otherUser = User.builder()
                    .id(2L)
                    .name("Other User")
                    .email("other@example.com")
                    .build();

            Client otherClient = Client.builder()
                    .id(1L)
                    .name("Other Client")
                    .email("otherclient@example.com")
                    .user(otherUser)
                    .build();

            when(clientRepository.findById(1L)).thenReturn(Optional.of(otherClient));

            assertThatThrownBy(() -> clientService.getClientById(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get All Clients Tests")
    class GetAllClientsTests {

        @Test
        @DisplayName("Should return all clients for user")
        void shouldReturnAllClientsForUser() {
            when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Arrays.asList(testClient1, testClient2));
            when(clientMapper.toResponse(testClient1)).thenReturn(ClientResponse.builder()
                    .id(1L)
                    .name("Client 1")
                    .email("client1@example.com")
                    .build());
            when(clientMapper.toResponse(testClient2)).thenReturn(ClientResponse.builder()
                    .id(2L)
                    .name("Client 2")
                    .email("client2@example.com")
                    .build());

            List<ClientResponse> result = clientService.getAllClients(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Client 1");
            assertThat(result.get(1).getName()).isEqualTo("Client 2");

            verify(clientRepository).findByUserIdOrderByCreatedAtDesc(1L);
            verify(clientMapper).toResponse(testClient1);
            verify(clientMapper).toResponse(testClient2);
        }

        @Test
        @DisplayName("Should return empty list when user has no clients")
        void shouldReturnEmptyListWhenUserHasNoClients() {
            when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Arrays.asList());

            List<ClientResponse> result = clientService.getAllClients(1L);

            assertThat(result).isEmpty();

            verify(clientRepository).findByUserIdOrderByCreatedAtDesc(1L);
        }
    }

    @Nested
    @DisplayName("Update Client Tests")
    class UpdateClientTests {

        @Test
        @DisplayName("Should update client when it exists and belongs to user")
        void shouldUpdateClientWhenItExistsAndBelongsToUser() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient1));
            when(clientMapper.toEntity(clientRequest, testClient1)).thenReturn(testClient1);
            when(clientRepository.save(any(Client.class))).thenReturn(testClient1);
            when(clientMapper.toResponse(testClient1)).thenReturn(ClientResponse.builder()
                    .id(1L)
                    .name("Client 1")
                    .email("client1@example.com")
                    .build());

            ClientResponse result = clientService.updateClient(1L, clientRequest, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Client 1");

            verify(clientRepository).findById(1L);
            verify(clientMapper).toEntity(clientRequest, testClient1);
            verify(clientRepository).save(any(Client.class));
            verify(clientMapper).toResponse(testClient1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientDoesNotExistForUpdate() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClient(1L, clientRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).findById(1L);
            verify(clientRepository, never()).save(any(Client.class));
        }
    }

    @Nested
    @DisplayName("Delete Client Tests")
    class DeleteClientTests {

        @Test
        @DisplayName("Should delete client when it exists and belongs to user")
        void shouldDeleteClientWhenItExistsAndBelongsToUser() {
            when(clientRepository.existsByIdAndUserId(1L, 1L)).thenReturn(true);

            clientService.deleteClient(1L, 1L);

            verify(clientRepository).existsByIdAndUserId(1L, 1L);
            verify(clientRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client does not exist for deletion")
        void shouldThrowExceptionWhenClientDoesNotExistForDeletion() {
            when(clientRepository.existsByIdAndUserId(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> clientService.deleteClient(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Client not found with id: 1");

            verify(clientRepository).existsByIdAndUserId(1L, 1L);
            verify(clientRepository, never()).deleteById(anyLong());
        }
    }
}
