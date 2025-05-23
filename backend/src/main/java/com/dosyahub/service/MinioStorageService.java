package com.dosyahub.service;

import com.dosyahub.model.FileMetadata;
import com.dosyahub.model.FileType;
import com.dosyahub.model.User;
import com.dosyahub.repository.FileMetadataRepository;
import com.dosyahub.repository.UserRepository;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            // Bucket yoksa oluştur
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket oluşturuldu: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket oluşturma hatası", e);
            throw new RuntimeException("MinIO başlatma hatası", e);
        }
    }

    /**
     * Dosya yükleme
     * @param userId Kullanıcı ID
     * @param file Yüklenecek dosya
     * @return Saklanan dosya adı (kullanıcı ID ve UUID ile)
     */
    public String storeFile(UUID userId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Yüklenecek dosya boş");
            }

            String originalFilename = file.getOriginalFilename();
            String objectName = userId + "/" + UUID.randomUUID() + "_" + originalFilename;

            // MinIO'ya dosyayı yükle
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Dosya MinIO'ya kaydedildi: {}", objectName);

            User user = userRepository.findById(userId).orElseThrow();
            FileMetadata metadata = FileMetadata.builder()
                .user(user)
                .originalFilename(originalFilename)
                .storedFilename(objectName)
                .contentType(file.getContentType())
                .fileType(getFileTypeFromContentType(file.getContentType()))
                .size(file.getSize())
                .bucketName(bucketName)
                .build();
            fileMetadataRepository.save(metadata);

            return objectName;
        } catch (Exception e) {
            log.error("MinIO dosya yükleme hatası", e);
            throw new RuntimeException("Dosya yükleme sırasında hata oluştu", e);
        }
    }

    /**
     * Dosya indirme - Tam dosya adı ile
     * @param objectName MinIO'daki nesne adı
     * @return Dosya içerik akışı
     */
    public InputStream getFileAsStream(String objectName) {
        try {
            // Dosya adını tam yola çevirelim
            String fullObjectName = objectName;
            
            // Eğer objectName zaten userId/ ile başlamıyorsa, kullanıcı ID'sini öne ekleyelim
            if (!objectName.contains("/")) {
                // Mevcut dosyayı bul
                FileMetadata metadata = fileMetadataRepository.findByStoredFilename(objectName)
                    .orElseGet(() -> {
                        // Doğrudan bulunamadıysa, dosya adının UUID_originalFilename formatında olduğunu varsay
                        if (objectName.contains("_")) {
                            // UUID kısmını al
                            String uuidPart = objectName.substring(0, objectName.indexOf('_'));
                            try {
                                // UUID formatını doğrula
                                UUID.fromString(uuidPart);
                                // Veritabanında ara
                                List<FileMetadata> matchingFiles = fileMetadataRepository.findByStoredFilenameContaining(objectName);
                                return matchingFiles.isEmpty() ? null : matchingFiles.get(0);
                            } catch (IllegalArgumentException e) {
                                // UUID formatı değilse boş dön
                                return null;
                            }
                        }
                        return null;
                    });
                
                if (metadata != null && metadata.getUser() != null) {
                    String userId = metadata.getUser().getId().toString();
                    fullObjectName = userId + "/" + objectName;
                    log.info("Tam dosya adı oluşturuldu: {}", fullObjectName);
                } else {
                    log.warn("Dosya veritabanında bulunamadı, doğrudan erişim denenecek: {}", objectName);
                }
            } else {
                log.info("Tam dosya yolu zaten mevcut: {}", fullObjectName);
            }
            
            // MinIO'dan dosyayı getir
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullObjectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO dosya indirme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Dosya indirme sırasında hata oluştu", e);
        }
    }

    /**
     * Dosya silme
     * @param objectName MinIO'daki nesne adı
     */
    public void deleteFile(String objectName) {
        try {
            // Dosya adından kullanıcı ID'yi çıkar (UUID formatında olabilir)
            // Bu kontrolü sadece güvenlik için yapıyoruz
            String userId = null;
            String fullObjectName = objectName;
            
            // Eğer objectName zaten userId/ ile başlamıyorsa, kullanıcı ID'sini öne ekleyelim
            if (!objectName.contains("/")) {
                // Mevcut dosyayı bul
                FileMetadata metadata = fileMetadataRepository.findByStoredFilename(objectName)
                    .orElseGet(() -> {
                        // Doğrudan bulunamadıysa, dosya adının UUID_originalFilename formatında olduğunu varsay
                        if (objectName.contains("_")) {
                            // UUID kısmını al
                            String uuidPart = objectName.substring(0, objectName.indexOf('_'));
                            try {
                                // UUID formatını doğrula
                                UUID.fromString(uuidPart);
                                // Veritabanında ara
                                List<FileMetadata> matchingFiles = fileMetadataRepository.findByStoredFilenameContaining(objectName);
                                return matchingFiles.isEmpty() ? null : matchingFiles.get(0);
                            } catch (IllegalArgumentException e) {
                                // UUID formatı değilse boş dön
                                return null;
                            }
                        }
                        return null;
                    });
                
                if (metadata != null && metadata.getUser() != null) {
                    userId = metadata.getUser().getId().toString();
                    fullObjectName = userId + "/" + objectName;
                    log.info("Tam dosya adı oluşturuldu: {}", fullObjectName);
                } else {
                    log.warn("Dosya veritabanında bulunamadı, doğrudan silme denenecek: {}", objectName);
                }
            } else {
                log.info("Tam dosya adı zaten mevcut: {}", fullObjectName);
            }

            // MinIO'dan dosyayı sil
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullObjectName)
                            .build()
            );
            log.info("Dosya silindi: {}", fullObjectName);
            
            // Veritabanı kaydını sil
            fileMetadataRepository.findByStoredFilename(objectName)
                .ifPresent(fileMetadataRepository::delete);
            
            // Eğer tam yol ile kaydedilmişse o kaydı da sil
            if (!objectName.equals(fullObjectName)) {
                fileMetadataRepository.findByStoredFilename(fullObjectName)
                    .ifPresent(fileMetadataRepository::delete);
            }
        } catch (Exception e) {
            log.error("Dosya silme hatası", e);
            throw new RuntimeException("Dosya silme sırasında hata oluştu", e);
        }
    }

    /**
     * Kullanıcının dosyalarını listeleme (sayfalama ve sıralama ile)
     * @param userId Kullanıcı ID
     * @param pageable Sayfalama ve sıralama bilgileri
     * @param search Arama metni (dosya adında arama)
     * @return Dosya bilgileri listesi
     */
    public List<Map<String, Object>> listFiles(UUID userId, Pageable pageable, String search) {
        try {
            String prefix = userId + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            // Sonuçları bir listeye dönüştür ve filtrele
            List<Map<String, Object>> filesList = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get();
                        } catch (Exception e) {
                            log.error("MinIO dosya bilgisi okuma hatası", e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .filter(item -> {
                        // Dosyaları filtrele
                        String objectName = item.objectName();
                        
                        // Arama metni varsa dosya adında ara
                        if (search != null && !search.isEmpty()) {
                            return objectName.toLowerCase().contains(search.toLowerCase());
                        }
                        
                        return true;
                    })
                    .map(item -> {
                        // Dosya bilgilerini bir Map olarak döndür
                        Map<String, Object> fileInfo = new HashMap<>();
                        String fileName = item.objectName().substring(prefix.length()); // kullanıcı ID kısmını çıkar
                        
                        // UUID kısmını ve orijinal dosya adını ayır
                        String uuid = "";
                        String originalName = fileName;
                        if (fileName.contains("_")) {
                            uuid = fileName.substring(0, fileName.indexOf('_'));
                            originalName = fileName.substring(fileName.indexOf('_') + 1);
                        }
                        
                        fileInfo.put("id", uuid);
                        fileInfo.put("filename", item.objectName());
                        fileInfo.put("originalName", originalName);
                        fileInfo.put("size", item.size());
                        fileInfo.put("contentType", getContentTypeFromFileName(originalName));
                        fileInfo.put("uploadDate", item.lastModified().toString());
                        fileInfo.put("isPublic", false);  // Varsayılan olarak özel
                        fileInfo.put("ownerId", userId.toString());
                        
                        return fileInfo;
                    })
                    .sorted(getComparator(pageable.getSort())) // Sıralama
                    .skip(pageable.getOffset()) // Sayfalama
                    .limit(pageable.getPageSize())
                    .collect(Collectors.toList());

            return filesList;
        } catch (Exception e) {
            log.error("MinIO dosya listeleme hatası", e);
            throw new RuntimeException("Dosya listeleme sırasında hata oluştu", e);
        }
    }

    /**
     * Kullanıcı dosyalarının sayısını sayma
     * @param userId Kullanıcı ID
     * @param search Arama metni
     * @return Dosya sayısı
     */
    public long countFiles(UUID userId, String search) {
        try {
            String prefix = userId + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            // Sonuçları say
            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .filter(item -> {
                        // Arama metni varsa dosya adında ara
                        if (search != null && !search.isEmpty()) {
                            return item.objectName().toLowerCase().contains(search.toLowerCase());
                        }
                        return true;
                    })
                    .count();
        } catch (Exception e) {
            log.error("MinIO dosya sayma hatası", e);
            throw new RuntimeException("Dosya sayma sırasında hata oluştu", e);
        }
    }

    /**
     * Kullanıcının dosyalarını listeleme
     * @param userId Kullanıcı ID
     * @return Dosya adlarının listesi
     */
    public List<String> listUserFiles(UUID userId) {
        List<String> fileNames = new ArrayList<>();
        try {
            String prefix = userId + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                fileNames.add(item.objectName());
            }
            return fileNames;
        } catch (Exception e) {
            log.error("MinIO dosya listeleme hatası", e);
            throw new RuntimeException("Dosya listeleme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya sıralama için sıralayıcı oluştur
     * @param sort Sıralama kriterleri
     * @return Dosya bilgisi sıralayıcısı
     */
    private Comparator<Map<String, Object>> getComparator(Sort sort) {
        if (sort.isUnsorted()) {
            // Varsayılan olarak yükleme tarihine göre azalan sıralama
            return (f1, f2) -> {
                String date1 = (String) f1.get("uploadDate");
                String date2 = (String) f2.get("uploadDate");
                return date2.compareTo(date1); // Azalan sırada
            };
        }
        
        // Dosya özelliklerine göre sıralama
        return (f1, f2) -> {
            for (Sort.Order order : sort) {
                int result = 0;
                String property = order.getProperty();
                
                // Özelliğe göre sıralama
                if (f1.containsKey(property) && f2.containsKey(property)) {
                    Object val1 = f1.get(property);
                    Object val2 = f2.get(property);
                    
                    if (val1 instanceof String && val2 instanceof String) {
                        result = ((String) val1).compareTo((String) val2);
                    } else if (val1 instanceof Number && val2 instanceof Number) {
                        result = Double.compare(((Number) val1).doubleValue(), ((Number) val2).doubleValue());
                    }
                }
                
                if (result != 0) {
                    return order.isAscending() ? result : -result;
                }
            }
            
            return 0;
        };
    }
    
    /**
     * Dosya adından MIME türünü tahmin et
     * @param fileName Dosya adı
     * @return MIME türü
     */
    private String getContentTypeFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty() || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".pdf":
                return "application/pdf";
            case ".txt":
                return "text/plain";
            case ".html":
                return "text/html";
            case ".json":
                return "application/json";
            case ".doc":
            case ".docx":
                return "application/msword";
            case ".xls":
            case ".xlsx":
                return "application/vnd.ms-excel";
            case ".ppt":
            case ".pptx":
                return "application/vnd.ms-powerpoint";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            case ".mp3":
                return "audio/mpeg";
            case ".mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * İçerik türünden dosya tipini belirle
     * @param contentType İçerik türü
     * @return Dosya tipi
     */
    private FileType getFileTypeFromContentType(String contentType) {
        if (contentType == null) {
            return FileType.PDF; // Varsayılan olarak PDF
        }
        
        if (contentType.contains("pdf")) {
            return FileType.PDF;
        } else if (contentType.contains("png")) {
            return FileType.PNG;
        } else if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return FileType.JPG;
        } else {
            // Desteklenmeyen dosya türü için varsayılan
            return FileType.PDF;
        }
    }
} 