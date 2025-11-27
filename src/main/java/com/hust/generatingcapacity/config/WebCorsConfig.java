package com.hust.generatingcapacity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig {
    @Bean
    public WebMvcConfigurer webCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")// 允许所有路径跨域
                        .allowedOriginPatterns("*")  // 允许所有前端访问
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")          // 允许所有请求头
                        .allowCredentials(true);
            }
        };
    }
}