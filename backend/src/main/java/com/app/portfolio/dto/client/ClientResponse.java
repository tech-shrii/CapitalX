package com.app.portfolio.dto.client;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ClientResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;
}
