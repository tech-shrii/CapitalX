package com.app.portfolio.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Client email is required")
    @Email
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 3)
    private String currency;
}
