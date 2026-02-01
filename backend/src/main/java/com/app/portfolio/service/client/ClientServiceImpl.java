package com.app.portfolio.service.client;

import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;
import com.app.portfolio.exceptions.ResourceNotFoundException;
import com.app.portfolio.mapper.ClientMapper;
import com.app.portfolio.repository.ClientRepository;
import com.app.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients(Long userId) {
        return clientRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(clientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id, Long userId) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", id);
        }
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse createClient(ClientRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Client client = clientMapper.toEntity(request, null);
        client.setUser(user);
        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request, Long userId) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
        if (!client.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Client", id);
        }
        client = clientMapper.toEntity(request, client);
        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional
    public void deleteClient(Long id, Long userId) {
        if (!clientRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Client", id);
        }
        clientRepository.deleteById(id);
    }
}
