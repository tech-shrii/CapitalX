package com.app.portfolio.dto.news;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NewsItemResponse {

    private String title;
    private String description;
    private String url;
    private String source;
    private Instant publishedAt;
    private String imageUrl;
}
