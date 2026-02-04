package com.app.portfolio.controller;

import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.client.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private ClientController clientController;

    @org.junit.jupiter.api.Test
    @DisplayName("Get all clients returns list of clients for valid user")
    void getAllClients_ReturnsListOfClients_ForValidUser() {
        long userId = 1L;
        List<ClientResponse> clients = List.of(mock(ClientResponse.class));
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.getAllClients(userId)).thenReturn(clients);

        ResponseEntity<List<ClientResponse>> response = clientController.getAllClients(userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(clients);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get client by id returns client for valid id and user")
    void getClientById_ReturnsClient_ForValidIdAndUser() {
        long userId = 1L;
        long clientId = 2L;
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.getClientById(clientId, userId)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = clientController.getClientById(clientId, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Create client returns created client for valid request")
    void createClient_ReturnsCreatedClient_ForValidRequest() {
        long userId = 1L;
        ClientRequest request = mock(ClientRequest.class);
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.createClient(request, userId)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = clientController.createClient(request, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Update client returns updated client for valid id and request")
    void updateClient_ReturnsUpdatedClient_ForValidIdAndRequest() {
        long userId = 1L;
        long clientId = 2L;
        ClientRequest request = mock(ClientRequest.class);
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.updateClient(clientId, request, userId)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> response = clientController.updateClient(clientId, request, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(clientResponse);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Delete client returns success message for valid id")
    void deleteClient_ReturnsSuccessMessage_ForValidId() {
        long userId = 1L;
        long clientId = 2L;
        when(userPrincipal.getId()).thenReturn(userId);
        doNothing().when(clientService).deleteClient(clientId, userId);

        ResponseEntity<?> response = clientController.deleteClient(clientId, userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(((Map<?, ?>) response.getBody()).get("message")).isEqualTo("Client deleted successfully");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get client by id throws exception when service throws exception")
    void getClientById_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        long clientId = 2L;
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.getClientById(clientId, userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> clientController.getClientById(clientId, userPrincipal));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Create client throws exception when service throws exception")
    void createClient_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        ClientRequest request = mock(ClientRequest.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.createClient(request, userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> clientController.createClient(request, userPrincipal));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Update client throws exception when service throws exception")
    void updateClient_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        long clientId = 2L;
        ClientRequest request = mock(ClientRequest.class);
        when(userPrincipal.getId()).thenReturn(userId);
        when(clientService.updateClient(clientId, request, userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> clientController.updateClient(clientId, request, userPrincipal));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Delete client throws exception when service throws exception")
    void deleteClient_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        long clientId = 2L;
        when(userPrincipal.getId()).thenReturn(userId);
        doThrow(new RuntimeException("error")).when(clientService).deleteClient(clientId, userId);

        assertThrows(RuntimeException.class, () -> clientController.deleteClient(clientId, userPrincipal));
    }
}
