package com.app.portfolio.controller;

import com.app.portfolio.dto.chatbot.ChatbotRequest;
import com.app.portfolio.dto.chatbot.ChatbotResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.chatbot.GeminiChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final GeminiChatbotService chatbotService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatbotResponse> chat(
            @Valid @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("POST /api/chatbot/chat - userId: {}", userPrincipal.getId());
        return ResponseEntity.ok(chatbotService.chat(userPrincipal.getId(), request));
    }
    
    @PostMapping("/greeting")
    public ResponseEntity<ChatbotResponse> greeting(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("POST /api/chatbot/greeting - userId: {}", userPrincipal.getId());
        return ResponseEntity.ok(chatbotService.generateGreeting(userPrincipal.getId()));
    }
    
    @PostMapping("/analysis")
    public ResponseEntity<ChatbotResponse> analysis(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("POST /api/chatbot/analysis - userId: {}", userPrincipal.getId());
        return ResponseEntity.ok(chatbotService.generatePortfolioAnalysis(userPrincipal.getId()));
    }
}
