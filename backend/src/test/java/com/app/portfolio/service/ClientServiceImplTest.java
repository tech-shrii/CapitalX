package com.app.portfolio.service;

import com.app.portfolio.beans.*;
import com.app.portfolio.dto.client.*;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.ClientMapper;
import com.app.portfolio.repository.*;
import com.app.portfolio.service.client.ClientServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientMapper clientMapper;

    @InjectMocks private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllClients_shouldReturnListOfClientResponses() {
        Client client = new Client();
        ClientResponse response = mock(ClientResponse.class);
        when(clientRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        List<ClientResponse> result = clientService.getAllClients(1L);

        assertThat(result).containsExactly(response);
    }

    @Test
    void getClientById_shouldReturnClientResponse_whenClientBelongsToUser() {
        Client client = new Client();
        User user = new User();
        user.setId(2L);
        client.setUser(user);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(mock(ClientResponse.class));

        ClientResponse result = clientService.getClientById(1L, 2L);

        assertThat(result).isNotNull();
    }

    @Test
    void getClientById_shouldThrow_whenClientNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> clientService.getClientById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getClientById_shouldThrow_whenClientNotBelongToUser() {
        Client client = new Client();
        User user = new User();
        user.setId(99L);
        client.setUser(user);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        assertThatThrownBy(() -> clientService.getClientById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createClient_shouldReturnClientResponse_whenValid() {
        User user = new User();
        Client client = new Client();
        ClientResponse response = mock(ClientResponse.class);
        ClientRequest request = new ClientRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(clientMapper.toEntity(request, null)).thenReturn(client);
        when(clientRepository.save(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.createClient(request, 1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void createClient_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> clientService.createClient(new ClientRequest(), 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateClient_shouldReturnClientResponse_whenValid() {
        Client client = new Client();
        User user = new User();
        user.setId(1L);
        client.setUser(user);
        ClientRequest request = new ClientRequest();
        Client updatedClient = new Client();
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientMapper.toEntity(request, client)).thenReturn(updatedClient);
        when(clientRepository.save(updatedClient)).thenReturn(updatedClient);
        when(clientMapper.toResponse(updatedClient)).thenReturn(response);

        ClientResponse result = clientService.updateClient(1L, request, 1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateClient_shouldThrow_whenClientNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> clientService.updateClient(1L, new ClientRequest(), 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateClient_shouldThrow_whenClientNotBelongToUser() {
        Client client = new Client();
        User user = new User();
        user.setId(99L);
        client.setUser(user);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        assertThatThrownBy(() -> clientService.updateClient(1L, new ClientRequest(), 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteClient_shouldDelete_whenClientBelongsToUser() {
        when(clientRepository.existsByIdAndUserId(1L, 2L)).thenReturn(true);
        clientService.deleteClient(1L, 2L);
        verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteClient_shouldThrow_whenClientNotBelongToUser() {
        when(clientRepository.existsByIdAndUserId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> clientService.deleteClient(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
