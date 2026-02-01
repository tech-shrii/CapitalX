package com.app.portfolio.controller;

import com.app.portfolio.dto.news.NewsItemResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<List<NewsItemResponse>> getFinancialNews(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(newsService.getFinancialNews(userPrincipal.getId()));
    }
}
