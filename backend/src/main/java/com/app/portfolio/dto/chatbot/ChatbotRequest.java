package com.app.portfolio.dto.chatbot;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatbotRequest {
    @NotBlank
    private String message;
    private String conversationId; // Optional: for conversation context
}
