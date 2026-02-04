package com.app.portfolio.controller;

import com.app.portfolio.dto.client.ClientResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.client.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal mockUserPrincipal;

    @BeforeEach
    void setUp() {
        mockUserPrincipal = new UserPrincipal(1L, "test@example.com", "password", Collections.emptyList());
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor testUser() {
        return SecurityMockMvcRequestPostProcessors.user(mockUserPrincipal);
    }

    @Test
    void getAllClients_shouldReturnClientList() throws Exception {
        Long userId = mockUserPrincipal.getId();
        ClientResponse clientResponse = ClientResponse.builder().id(1L).name("Test Client").email("client@example.com").build();
        List<ClientResponse> clientList = Collections.singletonList(clientResponse);

        when(clientService.getAllClients(eq(userId))).thenReturn(clientList);

        mockMvc.perform(get("/api/clients")
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Test Client"))
                .andExpect(jsonPath("$[0].email").value("client@example.com"));
    }

    @Test
    void getClientById_shouldReturnClient_whenFound() throws Exception {
        Long clientId = 1L;
        Long userId = mockUserPrincipal.getId();
        ClientResponse clientResponse = ClientResponse.builder().id(clientId).name("Test Client").email("client@example.com").build();

        when(clientService.getClientById(eq(clientId), eq(userId))).thenReturn(clientResponse);

        mockMvc.perform(get("/api/clients/{id}", clientId)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.name").value("Test Client"))
                .andExpect(jsonPath("$.email").value("client@example.com"));
    }

    @Test
    void getClientById_shouldReturnNotFound_whenClientNotFound() throws Exception {
        Long clientId = 99L;
        Long userId = mockUserPrincipal.getId();

        when(clientService.getClientById(eq(clientId), eq(userId)))
                .thenThrow(new ResourceNotFoundException("Client", clientId));

        mockMvc.perform(get("/api/clients/{id}", clientId)
                        .with(testUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Client not found with id '99'"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
