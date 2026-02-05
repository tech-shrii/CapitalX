package com.app.portfolio.service.news;

import com.app.portfolio.beans.Asset;
import com.app.portfolio.dto.news.NewsItemResponse;
import com.app.portfolio.repository.AssetRepository;
import com.app.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${app.news.api-key:your-newsapi-key}")
    private String newsApiKey;

    @Value("${app.news.api-url:https://newsapi.org/v2/everything}")
    private String newsApiUrl;

    @Override
    @Transactional(readOnly = true)
    public List<NewsItemResponse> getFinancialNews(Long userId) {
        // Extract keywords from user's assets
        List<String> keywords = extractKeywordsFromAssets(userId);
        if (keywords.isEmpty()) {
            keywords = Arrays.asList("finance", "stocks", "investment", "market");
        }

        String query = String.join(" OR ", keywords);
        return fetchNewsFromApi(query);
    }

    private List<String> extractKeywordsFromAssets(Long userId) {
        Set<String> keywords = new HashSet<>();
        
        // Get all assets for the user
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(asset -> asset.getClient().getUser().getId().equals(userId))
                .collect(Collectors.toList());

        for (Asset asset : assets) {
            if (asset.getSymbol() != null && !asset.getSymbol().isEmpty()) {
                keywords.add(asset.getSymbol());
            }
            if (asset.getName() != null && !asset.getName().isEmpty()) {
                String[] nameParts = asset.getName().split("\\s+");
                keywords.addAll(Arrays.asList(nameParts));
            }
            keywords.add(asset.getCategory().name());
        }

        return new ArrayList<>(keywords);
    }

    private List<NewsItemResponse> fetchNewsFromApi(String query) {
        try {
            String url = newsApiUrl + "?q=" + query + "&apiKey=" + newsApiKey + "&language=en&sortBy=publishedAt&pageSize=20";
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("articles")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                
                return articles.stream()
                        .map(this::mapToNewsItem)
                        .filter(Objects::nonNull)
                        .limit(20)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to fetch news from API: {}", e.getMessage());
        }
        
        // Return mock data if API fails
        return generateMockNews();
    }

    @SuppressWarnings("unchecked")
    private NewsItemResponse mapToNewsItem(Map<String, Object> article) {
        try {
            String title = (String) article.get("title");
            String description = (String) article.get("description");
            String url = (String) article.get("url");
            String imageUrl = (String) article.get("urlToImage");
            
            Map<String, String> source = (Map<String, String>) article.get("source");
            String sourceName = source != null ? source.get("name") : "Unknown";
            
            String publishedAtStr = (String) article.get("publishedAt");
            Instant publishedAt = publishedAtStr != null 
                    ? Instant.parse(publishedAtStr) 
                    : Instant.now();

            return NewsItemResponse.builder()
                    .title(title != null ? title : "No title")
                    .description(description != null ? description : "")
                    .url(url != null ? url : "")
                    .source(sourceName)
                    .publishedAt(publishedAt)
                    .imageUrl(imageUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error mapping news item: {}", e.getMessage());
            return null;
        }
    }

    private List<NewsItemResponse> generateMockNews() {
        return Arrays.asList(
                NewsItemResponse.builder()
                        .title("Market Update: Stocks Show Strong Performance")
                        .description("Financial markets continue to show resilience amid economic changes.")
                        .url("https://example.com/news/1")
                        .source("Financial News")
                        .publishedAt(Instant.now())
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Investment Strategies for 2026")
                        .description("Experts share insights on portfolio management and asset allocation.")
                        .url("https://example.com/news/2")
                        .source("Investment Weekly")
                        .publishedAt(Instant.now().minusSeconds(3600))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Central Banks Hold Rates Steady")
                        .description("Major central banks signal a pause in rate hikes as inflation eases.")
                        .url("https://example.com/news/3")
                        .source("Reuters")
                        .publishedAt(Instant.now().minusSeconds(7200))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Tech Sector Earnings Beat Expectations")
                        .description("Leading technology companies report stronger-than-expected quarterly results.")
                        .url("https://example.com/news/4")
                        .source("Bloomberg")
                        .publishedAt(Instant.now().minusSeconds(10800))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Bond Yields Fall on Safe-Haven Demand")
                        .description("Government bond prices rise as investors seek safety amid market volatility.")
                        .url("https://example.com/news/5")
                        .source("Financial Times")
                        .publishedAt(Instant.now().minusSeconds(14400))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Crypto Markets See Renewed Interest")
                        .description("Digital asset trading volumes increase as institutional adoption grows.")
                        .url("https://example.com/news/6")
                        .source("CoinDesk")
                        .publishedAt(Instant.now().minusSeconds(18000))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Commodity Prices Stabilize After Volatile Week")
                        .description("Oil and metals find support as supply concerns ease.")
                        .url("https://example.com/news/7")
                        .source("Wall Street Journal")
                        .publishedAt(Instant.now().minusSeconds(21600))
                        .imageUrl("")
                        .build(),
                NewsItemResponse.builder()
                        .title("Mutual Fund Inflows Hit Record High")
                        .description("Investors pour into diversified funds amid uncertainty.")
                        .url("https://example.com/news/8")
                        .source("Morningstar")
                        .publishedAt(Instant.now().minusSeconds(25200))
                        .imageUrl("")
                        .build()
        );
    }
}
