package com.app.portfolio.service.news;

import com.app.portfolio.dto.news.NewsItemResponse;

import java.util.List;

public interface NewsService {

    List<NewsItemResponse> getFinancialNews(Long userId);
}
