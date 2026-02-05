package com.app.portfolio.service.chatbot;

import com.app.portfolio.beans.User;
import com.app.portfolio.dto.chatbot.ChatbotRequest;
import com.app.portfolio.dto.chatbot.ChatbotResponse;
import com.app.portfolio.dto.dashboard.DashboardSummaryResponse;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.service.dashboard.DashboardService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiChatbotServiceImpl implements GeminiChatbotService {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key2:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String geminiApiUrl;

    @Value("${gemini.api.enabled:true}")
    private boolean geminiApiEnabled;

    // Cache for portfolio summaries (refresh every 5 minutes)
    private final Map<Long, CachedPortfolioData> portfolioCache = new HashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponse chat(Long userId, ChatbotRequest request) {
        if (!geminiApiEnabled) {
            log.warn("Gemini API is disabled");
            return ChatbotResponse.builder()
                    .response("Chatbot service is currently unavailable. Please contact support.")
                    .conversationId(request.getConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }

        try {
            String portfolioData = getPortfolioDataAsMarkdown(userId);
            String systemPrompt = buildSystemPrompt(portfolioData, userId);
            String userMessage = request.getMessage();

            String fullPrompt = systemPrompt + "\n\n**User Question:**\n" + userMessage;

            String geminiResponse = callGeminiApi(fullPrompt);
            
            return ChatbotResponse.builder()
                    .response(geminiResponse)
                    .conversationId(request.getConversationId() != null ? request.getConversationId() : generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        } catch (Exception e) {
            log.error("Error in chat for userId: {}", userId, e);
            return ChatbotResponse.builder()
                    .response("I apologize, but I encountered an error processing your request. Please try again later.")
                    .conversationId(request.getConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponse generateGreeting(Long userId) {
        if (!geminiApiEnabled) {
            log.warn("Gemini API is disabled");
            return ChatbotResponse.builder()
                    .response("Welcome to CapitalX! I'm your investment expert assistant. How can I help you today?")
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }

        try {
            String portfolioData = getPortfolioDataAsMarkdown(userId);
            String greetingPrompt = buildGreetingPrompt(portfolioData, userId);

            String geminiResponse = callGeminiApi(greetingPrompt);
            
            return ChatbotResponse.builder()
                    .response(geminiResponse)
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        } catch (Exception e) {
            log.error("Error generating greeting for userId: {}", userId, e);
            return ChatbotResponse.builder()
                    .response("Welcome to CapitalX! I'm your investment expert assistant. How can I help you today?")
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotResponse generatePortfolioAnalysis(Long userId) {
        if (!geminiApiEnabled) {
            log.warn("Gemini API is disabled");
            return ChatbotResponse.builder()
                    .response("Portfolio analysis is currently unavailable. Please try again later.")
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }

        try {
            String portfolioData = getPortfolioDataAsMarkdown(userId);
            String analysisPrompt = buildAnalysisPrompt(portfolioData, userId);

            String geminiResponse = callGeminiApi(analysisPrompt);
            
            return ChatbotResponse.builder()
                    .response(geminiResponse)
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        } catch (Exception e) {
            log.error("Error generating portfolio analysis for userId: {}", userId, e);
            return ChatbotResponse.builder()
                    .response("I apologize, but I encountered an error analyzing your portfolio. Please try again later.")
                    .conversationId(generateConversationId())
                    .timestamp(Instant.now())
                    .role("assistant")
                    .build();
        }
    }

    private String getPortfolioDataAsMarkdown(Long userId) {
        // Check cache first
        CachedPortfolioData cached = portfolioCache.get(userId);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            log.debug("Using cached portfolio data for userId: {}", userId);
            return cached.data;
        }

        try {
            DashboardSummaryResponse summary = dashboardService.getDashboardSummary(userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            StringBuilder markdown = new StringBuilder();
            markdown.append("# Portfolio Summary for ").append(user.getName()).append("\n\n");
            markdown.append("## Overall Portfolio\n");
            markdown.append("- **Total Invested:** $").append(formatCurrency(summary.getTotalInvested())).append("\n");
            markdown.append("- **Current Value:** $").append(formatCurrency(summary.getTotalCurrentValue())).append("\n");
            markdown.append("- **Total P&L:** $").append(formatCurrency(summary.getTotalProfitLoss()))
                    .append(" (").append(formatPercentage(summary.getTotalProfitLossPercent())).append(")\n");
            markdown.append("- **Today's P&L:** $").append(formatCurrency(summary.getTodaysPL()))
                    .append(" (").append(formatPercentage(summary.getTodaysPLPercentage())).append(")\n");
            markdown.append("- **Total Clients:** ").append(summary.getTotalClients()).append("\n");
            markdown.append("- **Total Assets:** ").append(summary.getTotalAssets()).append("\n\n");

            if (summary.getRecentClients() != null && !summary.getRecentClients().isEmpty()) {
                markdown.append("## Clients Overview\n");
                for (DashboardSummaryResponse.ClientSummaryDto client : summary.getRecentClients()) {
                    markdown.append("### Client: ").append(client.getName()).append("\n");
                    markdown.append("- **Email:** ").append(client.getEmail()).append("\n");
                    markdown.append("- **Assets:** ").append(client.getAssetCount()).append(" assets\n");
                    markdown.append("- **P&L:** $").append(formatCurrency(client.getProfitLoss())).append("\n\n");
                }
            }

            if (summary.getAssetCategoryBreakdown() != null && !summary.getAssetCategoryBreakdown().isEmpty()) {
                markdown.append("## Asset Breakdown by Category\n");
                BigDecimal totalValue = summary.getTotalCurrentValue();
                for (Map.Entry<String, BigDecimal> entry : summary.getAssetCategoryBreakdown().entrySet()) {
                    String category = entry.getKey();
                    BigDecimal value = entry.getValue();
                    BigDecimal percentage = totalValue.compareTo(BigDecimal.ZERO) > 0 
                            ? value.divide(totalValue, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;
                    markdown.append("- **").append(category).append(":** $")
                            .append(formatCurrency(value)).append(" (").append(formatPercentage(percentage)).append(")\n");
                }
                markdown.append("\n");
            }

            if (summary.getTopAssets() != null && !summary.getTopAssets().isEmpty()) {
                markdown.append("## Top Performing Assets\n");
                int count = 1;
                for (DashboardSummaryResponse.TopAssetDto asset : summary.getTopAssets()) {
                    markdown.append(count++).append(". ").append(asset.getName())
                            .append(" (").append(asset.getCategory()).append(") - ")
                            .append(formatPercentage(asset.getReturns())).append("\n");
                }
                markdown.append("\n");
            }

            if (summary.getWorstAssets() != null && !summary.getWorstAssets().isEmpty()) {
                markdown.append("## Worst Performing Assets\n");
                int count = 1;
                for (DashboardSummaryResponse.TopAssetDto asset : summary.getWorstAssets()) {
                    markdown.append(count++).append(". ").append(asset.getName())
                            .append(" (").append(asset.getCategory()).append(") - ")
                            .append(formatPercentage(asset.getReturns())).append("\n");
                }
                markdown.append("\n");
            }

            String data = markdown.toString();
            
            // Cache the data
            portfolioCache.put(userId, new CachedPortfolioData(data, System.currentTimeMillis()));
            
            return data;
        } catch (Exception e) {
            log.error("Error building portfolio markdown for userId: {}", userId, e);
            return "# Portfolio Summary\n\nUnable to load portfolio data at this time.";
        }
    }

    private String buildSystemPrompt(String portfolioData, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return String.format("""
                You are an expert investment advisor and portfolio analyst. Your role is to:
                
                1. **Analyze Portfolios:** Provide detailed analysis of investment portfolios
                2. **Give Recommendations:** Suggest portfolio improvements, diversification, risk management
                3. **Answer Questions:** Answer investment-related questions based on the user's portfolio data
                4. **Educate:** Explain investment concepts in simple terms
                
                **Guidelines:**
                - Always base recommendations on the provided portfolio data
                - Consider diversification, risk tolerance, and market conditions
                - Be concise but thorough
                - Use emojis sparingly for better readability
                - Format responses with clear sections using Markdown
                - If asked about specific assets, reference the portfolio data provided
                
                **User's Portfolio Context:**
                %s
                
                **Current Date:** %s
                **User's Name:** %s
                """, portfolioData, LocalDate.now().format(DateTimeFormatter.ISO_DATE), user.getName());
    }

    private String buildGreetingPrompt(String portfolioData, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return String.format("""
                You are an expert investment advisor. Greet the user warmly and provide a brief summary of their portfolio:
                - Total portfolio value
                - Overall performance (P&L %%)
                - Number of clients and assets
                - One key insight or suggestion
                
                Keep it friendly, concise, and actionable. Use Markdown formatting for clarity.
                
                **User's Portfolio Context:**
                %s
                
                **Current Date:** %s
                **User's Name:** %s
                """, portfolioData, LocalDate.now().format(DateTimeFormatter.ISO_DATE), user.getName());
    }

    private String buildAnalysisPrompt(String portfolioData, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return String.format("""
                You are an expert investment advisor. Analyze the user's portfolio and provide:
                - Diversification assessment
                - Risk level evaluation
                - Top 3 strengths
                - Top 3 areas for improvement
                - Specific actionable recommendations
                
                Format your response with clear sections using Markdown. Be thorough but concise.
                
                **User's Portfolio Context:**
                %s
                
                **Current Date:** %s
                **User's Name:** %s
                """, portfolioData, LocalDate.now().format(DateTimeFormatter.ISO_DATE), user.getName());
    }

    private String callGeminiApi(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new RuntimeException("Gemini API key is not configured");
        }

        try {
            WebClient webClient = webClientBuilder.build();
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            content.put("role", "user"); // Add role as per GeminiTest.java
            requestBody.put("contents", List.of(content));

            // Add safety settings to avoid blocking due to financial data
            List<Map<String, String>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
            );
            requestBody.put("safetySettings", safetySettings);

            String url = geminiApiUrl + "?key=" + geminiApiKey;

            log.debug("Calling Gemini API: {}", url);

            String responseJson = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Gemini API response: {}", responseJson);

            JsonNode jsonNode = objectMapper.readTree(responseJson);
            JsonNode candidates = jsonNode.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode contentNode = candidates.get(0).get("content");
                if (contentNode != null) {
                    JsonNode parts = contentNode.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode textNode = parts.get(0).get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    }
                }
            }

            log.warn("Unexpected Gemini API response structure: {}", responseJson);
            return "I apologize, but I couldn't process the response. Please try again.";
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Error calling Gemini API. Status: {}, Body: {}, API KEY: {}", e.getStatusCode(), e.getResponseBodyAsString(), geminiApiKey,e);
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage() + ". Response Body: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toString();
    }

    private String formatPercentage(BigDecimal percentage) {
        if (percentage == null) {
            return "0.00%";
        }
        return percentage.setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    private String generateConversationId() {
        return "conv-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static class CachedPortfolioData {
        final String data;
        final long timestamp;

        CachedPortfolioData(String data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}