package com.dosyahub.service;

import com.dosyahub.exception.FileStorageException;
import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    /**
     * Uygulama başlangıcında bucket kontrolü
     * @throws MinioException MinIO hata durumunda fırlatılır
     */
    public void checkBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket '{}' oluşturuldu", bucketName);
            } else {
                log.info("Bucket '{}' zaten mevcut", bucketName);
            }
        } catch (Exception e) {
            log.error("Bucket kontrolü sırasında hata oluştu: {}", e.getMessage());
            throw new FileStorageException("MinIO bucket kontrolü sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya yükleme
     * @param userId Kullanıcı ID
     * @param file Yüklenecek dosya
     * @return Saklanan dosya adı (UUID ile)
     */
    public String uploadFile(UUID userId, MultipartFile file) {
        try {
            checkBucket();
            
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            String objectName = userId + "/" + storedFilename;
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            
            log.info("Dosya yüklendi: {}", objectName);
            return storedFilename;
            
        } catch (Exception e) {
            log.error("Dosya yükleme sırasında hata oluştu: {}", e.getMessage());
            throw new FileStorageException("Dosya yükleme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya indirme
     * @param userId Kullanıcı ID
     * @param storedFilename Saklanan dosya adı
     * @return Dosya içerik akışı
     */
    public InputStream downloadFile(UUID userId, String storedFilename) {
        try {
            String objectName = userId + "/" + storedFilename;
            
            GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            
            log.info("Dosya indirildi: {}", objectName);
            return stream;
            
        } catch (Exception e) {
            log.error("Dosya indirme sırasında hata oluştu: {}", e.getMessage());
            throw new FileStorageException("Dosya indirme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya silme
     * @param userId Kullanıcı ID
     * @param storedFilename Saklanan dosya adı
     */
    public void deleteFile(UUID userId, String storedFilename) {
        try {
            String objectName = userId + "/" + storedFilename;
            
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            
            log.info("Dosya silindi: {}", objectName);
            
        } catch (Exception e) {
            log.error("Dosya silme sırasında hata oluştu: {}", e.getMessage());
            throw new FileStorageException("Dosya silme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya uzantısını alma
     * @param filename Dosya adı
     * @return Dosya uzantısı
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
} 