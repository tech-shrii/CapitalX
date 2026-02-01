package com.app.portfolio.controller;

import com.app.portfolio.dto.statement.StatementRequest;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.statement.StatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateStatement(@Valid @RequestBody StatementRequest request,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {
        statementService.generateAndSendStatement(request, userPrincipal.getId());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Statement generated and sent successfully"));
    }
}
