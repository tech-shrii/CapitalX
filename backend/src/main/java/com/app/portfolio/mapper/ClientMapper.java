package com.app.portfolio.mapper;

import com.app.portfolio.beans.Client;
import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public Client toEntity(ClientRequest request, Client existing) {
        if (existing == null) {
            existing = new Client();
        }
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        return existing;
    }

    public ClientResponse toResponse(Client entity) {
        return ClientResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
