package com.app.portfolio.service.statement;

import com.app.portfolio.dto.statement.StatementRequest;

public interface StatementService {

    void generateAndSendStatement(StatementRequest request, Long userId);
}
