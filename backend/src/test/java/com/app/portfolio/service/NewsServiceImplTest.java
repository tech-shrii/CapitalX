package com.app.portfolio.service;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.beans.Client;
import com.app.portfolio.beans.User;
import com.app.portfolio.dto.news.NewsItemResponse;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.UserRepository;
import com.app.portfolio.service.news.NewsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewsServiceImplTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NewsServiceImpl newsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Fetch financial news with valid user ID and assets")
    void fetchFinancialNewsWithValidUserIdAndAssets() {
        Long userId = 1L;
        Asset asset = new Asset();
        asset.setSymbol("AAPL");
        asset.setName("Apple Inc");
        //asset.setCategory("STOCK");
        Client client = new Client();
        User user = new User();
        user.setId(userId);
        client.setUser(user);
        asset.setClient(client);

        when(assetRepository.findAll()).thenReturn(Collections.singletonList(asset));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockApiResponse());

        List<NewsItemResponse> news = newsService.getFinancialNews(userId);

        assertNotNull(news);
        assertFalse(news.isEmpty());
        assertEquals(2, news.size());
    }

    @Test
    @DisplayName("Fetch financial news with no assets for user")
    void fetchFinancialNewsWithNoAssetsForUser() {
        Long userId = 1L;

        when(assetRepository.findAll()).thenReturn(Collections.emptyList());
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockApiResponse());

        List<NewsItemResponse> news = newsService.getFinancialNews(userId);

        assertNotNull(news);
        assertFalse(news.isEmpty());
        assertEquals(2, news.size());
    }

    @Test
    @DisplayName("Fetch financial news when API fails")
    void fetchFinancialNewsWhenApiFails() {
        Long userId = 1L;

        when(assetRepository.findAll()).thenReturn(Collections.emptyList());
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("API error"));

        List<NewsItemResponse> news = newsService.getFinancialNews(userId);

        assertNotNull(news);
        assertFalse(news.isEmpty());
        assertEquals(2, news.size());
        assertEquals("Market Update: Stocks Show Strong Performance", news.get(0).getTitle());
    }

    private Map<String, Object> mockApiResponse() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> articles = new ArrayList<>();

        Map<String, Object> article1 = new HashMap<>();
        article1.put("title", "Market Update: Stocks Show Strong Performance");
        article1.put("description", "Financial markets continue to show resilience amid economic changes.");
        article1.put("url", "https://example.com/news/1");
        article1.put("urlToImage", "");
        article1.put("publishedAt", Instant.now().toString());
        article1.put("source", Map.of("name", "Financial News"));

        Map<String, Object> article2 = new HashMap<>();
        article2.put("title", "Investment Strategies for 2026");
        article2.put("description", "Experts share insights on portfolio management and asset allocation.");
        article2.put("url", "https://example.com/news/2");
        article2.put("urlToImage", "");
        article2.put("publishedAt", Instant.now().minusSeconds(3600).toString());
        article2.put("source", Map.of("name", "Investment Weekly"));

        articles.add(article1);
        articles.add(article2);
        response.put("articles", articles);

        return response;
    }
}
