package com.app.portfolio.dto.statement;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatementRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Statement type is required")
    private StatementType statementType;

    @Email(message = "Valid email is required")
    private String emailTo;

    public enum StatementType {
        BASIC_PNL, DETAILED
    }
}
