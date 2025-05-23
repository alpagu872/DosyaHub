package com.dosyahub.config;

import com.dosyahub.filter.CustomCorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/*")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition", "Content-Type", "Content-Length", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Bean
    public FilterRegistrationBean<CustomCorsFilter> customCorsFilterRegistration(CustomCorsFilter customCorsFilter) {
        FilterRegistrationBean<CustomCorsFilter> registrationBean = new FilterRegistrationBean<>(customCorsFilter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
} 