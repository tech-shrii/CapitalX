package com.app.portfolio.controller;

import com.app.portfolio.dto.client.ClientRequest;
import com.app.portfolio.dto.client.ClientResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.client.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.getAllClients(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.getClientById(id, userPrincipal.getId()));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.createClient(request, userPrincipal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(@PathVariable Long id,
                                                         @Valid @RequestBody ClientRequest request,
                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.updateClient(id, request, userPrincipal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        clientService.deleteClient(id, userPrincipal.getId());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Client deleted successfully"));
    }
}
