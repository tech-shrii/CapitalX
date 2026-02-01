package com.app.portfolio.service.client;

import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;

import java.util.List;

public interface ClientService {

    List<ClientResponse> getAllClients(Long userId);

    ClientResponse getClientById(Long id, Long userId);

    ClientResponse createClient(ClientRequest request, Long userId);

    ClientResponse updateClient(Long id, ClientRequest request, Long userId);

    void deleteClient(Long id, Long userId);
}
