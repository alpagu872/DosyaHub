package com.dosyahub.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {
    
    @Value("${minio.endpoint}")
    private String endpoint;
    
    @Value("${minio.port}")
    private int port;
    
    @Value("${minio.access-key}")
    private String accessKey;
    
    @Value("${minio.secret-key}")
    private String secretKey;
    
    @Value("${minio.secure}")
    private boolean secure;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.enabled}")
    private boolean enabled;
    
    @Bean
    public MinioClient minioClient() {
        if (!enabled) {
            log.info("MinIO devre dışı bırakıldı, dosya sistemi kullanılacak");
            return null;
        }

        try {
            log.info("MinIO istemcisi oluşturuluyor: endpoint={}, accessKey={}", endpoint, accessKey);
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            log.error("MinIO client oluşturulamadı", e);
            throw new RuntimeException("MinIO client oluşturulamadı", e);
        }
    }
} 