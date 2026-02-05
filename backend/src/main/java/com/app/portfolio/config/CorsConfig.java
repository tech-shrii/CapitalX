package com.app.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Note: CORS is primarily configured in SecurityConfig.java
 * This class provides additional WebMvc-level CORS configuration if needed.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:5500",
                    "http://localhost:5501",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:5500",
                    "http://127.0.0.1:5501",
                    "http://localhost:8080",
                    "https://capitalx.shreyask.in",
                    "http://capitalx.shreyask.in"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
