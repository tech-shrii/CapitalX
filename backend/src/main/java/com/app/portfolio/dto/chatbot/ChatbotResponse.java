package com.app.portfolio.dto.chatbot;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ChatbotResponse {
    private String response;
    private String conversationId;
    private Instant timestamp;
    private String role; // "assistant" or "user"
}
