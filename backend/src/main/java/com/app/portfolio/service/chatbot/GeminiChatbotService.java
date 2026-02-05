package com.app.portfolio.service.chatbot;

import com.app.portfolio.dto.chatbot.ChatbotRequest;
import com.app.portfolio.dto.chatbot.ChatbotResponse;

public interface GeminiChatbotService {
    /**
     * Send user message to Gemini and get investment advice
     */
    ChatbotResponse chat(Long userId, ChatbotRequest request);
    
    /**
     * Generate initial greeting and portfolio summary on login
     */
    ChatbotResponse generateGreeting(Long userId);
    
    /**
     * Generate portfolio analysis and suggestions
     */
    ChatbotResponse generatePortfolioAnalysis(Long userId);
}
