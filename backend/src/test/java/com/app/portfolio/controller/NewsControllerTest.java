package com.app.portfolio.controller;

import com.app.portfolio.dto.news.NewsItemResponse;
import com.app.portfolio.security.UserPrincipal;
import com.app.portfolio.service.news.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private NewsController newsController;

    @org.junit.jupiter.api.Test
    @DisplayName("Get financial news returns list for valid user")
    void getFinancialNews_ReturnsList_ForValidUser() {
        long userId = 1L;
        List<NewsItemResponse> newsList = List.of(mock(NewsItemResponse.class));
        when(userPrincipal.getId()).thenReturn(userId);
        when(newsService.getFinancialNews(userId)).thenReturn(newsList);

        ResponseEntity<List<NewsItemResponse>> response = newsController.getFinancialNews(userPrincipal);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(newsList);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Get financial news throws exception when service throws exception")
    void getFinancialNews_ThrowsException_WhenServiceThrowsException() {
        long userId = 1L;
        when(userPrincipal.getId()).thenReturn(userId);
        when(newsService.getFinancialNews(userId)).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> newsController.getFinancialNews(userPrincipal));
    }
}
