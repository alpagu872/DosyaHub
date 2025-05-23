package com.dosyahub.service;

import com.dosyahub.exception.FileStorageException;
import com.dosyahub.model.FileMetadata;
import com.dosyahub.model.FileType;
import com.dosyahub.model.User;
import com.dosyahub.repository.FileMetadataRepository;
import com.dosyahub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    @Value("${storage.upload-dir:uploads}")
    private String uploadDir;

    private Path rootLocation;
    
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    
    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir);
            Files.createDirectories(rootLocation);
            log.info("Yükleme dizini oluşturuldu: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new FileStorageException("Yükleme dizini oluşturulamadı", e);
        }
    }
    
    /**
     * Dosya yükleme
     * @param userId Kullanıcı ID
     * @param file Yüklenecek dosya
     * @return Saklanan dosya adı (UUID ile)
     */
    public String storeFile(UUID userId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new FileStorageException("Yüklenecek dosya boş");
            }
            
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            
            // Kullanıcı dizinini oluştur
            Path userDir = rootLocation.resolve(userId.toString());
            Files.createDirectories(userDir);
            
            // Dosyayı kaydet
            Path targetLocation = userDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Dosya kaydedildi: {}", targetLocation);
            
            // Veritabanına kaydet
            User user = userRepository.findById(userId).orElseThrow();
            FileMetadata metadata = FileMetadata.builder()
                .user(user)
                .originalFilename(originalFilename)
                .storedFilename(userId + "/" + storedFilename)
                .contentType(file.getContentType())
                .fileType(getFileTypeFromContentType(file.getContentType()))
                .size(file.getSize())
                .bucketName("filesystem")
                .build();
            fileMetadataRepository.save(metadata);
            
            return storedFilename;
            
        } catch (IOException e) {
            throw new FileStorageException("Dosya yükleme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Dosya indirme
     * @param userId Kullanıcı ID
     * @param storedFilename Saklanan dosya adı
     * @return Dosya içerik akışı
     */
    public InputStream getFileAsStream(UUID userId, String storedFilename) {
        try {
            Path userDir = rootLocation.resolve(userId.toString());
            Path filePath = userDir.resolve(storedFilename);
            
            if (!Files.exists(filePath)) {
                throw new FileStorageException("Dosya bulunamadı: " + storedFilename);
            }
            
            return Files.newInputStream(filePath);
            
        } catch (IOException e) {
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
            Path userDir = rootLocation.resolve(userId.toString());
            Path filePath = userDir.resolve(storedFilename);
            
            if (!Files.exists(filePath)) {
                throw new FileStorageException("Dosya bulunamadı: " + storedFilename);
            }
            
            Files.delete(filePath);
            log.info("Dosya silindi: {}", filePath);
            
            // Veritabanından dosya kaydını sil
            fileMetadataRepository.findByStoredFilename(userId + "/" + storedFilename)
                .ifPresent(fileMetadataRepository::delete);
            
        } catch (IOException e) {
            throw new FileStorageException("Dosya silme sırasında hata oluştu", e);
        }
    }
    
    /**
     * Kullanıcı dosyalarını listeleme
     * @param userId Kullanıcı ID
     * @param pageable Sayfalama ve sıralama
     * @param search Arama metni
     * @return Dosya nesneleri listesi
     */
    public List<Map<String, Object>> listFiles(UUID userId, Pageable pageable, String search) {
        try {
            Path userDir = rootLocation.resolve(userId.toString());
            
            // Kullanıcı dizini yoksa oluştur
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> filesList = new ArrayList<>();
            
            try (Stream<Path> filesStream = Files.list(userDir)) {
                filesList = filesStream
                    .filter(path -> {
                        try {
                            // Sadece dosyaları filtrele
                            if (!Files.isRegularFile(path)) {
                                return false;
                            }
                            
                            // Arama metni varsa dosya adında ara
                            if (search != null && !search.isEmpty()) {
                                return path.getFileName().toString().toLowerCase()
                                    .contains(search.toLowerCase());
                            }
                            
                            return true;
                        } catch (Exception e) {
                            log.error("Dosya filtreleme hatası", e);
                            return false;
                        }
                    })
                    .sorted(getComparator(pageable.getSort()))
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .map(path -> {
                        try {
                            String filename = path.getFileName().toString();
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            
                            // Orijinal dosya adını al (UUID kısmını çıkar)
                            String originalName = filename;
                            if (filename.contains("_")) {
                                originalName = filename.substring(filename.indexOf('_') + 1);
                            }
                            
                            // Dosya bilgilerini topla
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("id", path.getFileName().toString().split("_")[0]);  // UUID kısmı
                            fileInfo.put("filename", filename);
                            fileInfo.put("originalName", originalName);
                            fileInfo.put("size", Files.size(path));
                            fileInfo.put("contentType", getContentType(path));
                            fileInfo.put("uploadDate", attrs.creationTime().toInstant().toString());
                            fileInfo.put("isPublic", false);  // Varsayılan olarak dosyalar özel
                            fileInfo.put("ownerId", userId.toString());
                            
                            return fileInfo;
                        } catch (IOException e) {
                            log.error("Dosya bilgisi okuma hatası", e);
                            return null;
                        }
                    })
                    .filter(fileInfo -> fileInfo != null)
                    .collect(Collectors.toList());
            }
            
            return filesList;
        } catch (IOException e) {
            log.error("Dosya listeleme hatası", e);
            throw new FileStorageException("Dosyaları listelerken hata oluştu", e);
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
            Path userDir = rootLocation.resolve(userId.toString());
            
            // Kullanıcı dizini yoksa sıfır döndür
            if (!Files.exists(userDir)) {
                return 0;
            }
            
            try (Stream<Path> filesStream = Files.list(userDir)) {
                return filesStream
                    .filter(path -> {
                        try {
                            // Sadece dosyaları filtrele
                            if (!Files.isRegularFile(path)) {
                                return false;
                            }
                            
                            // Arama metni varsa dosya adında ara
                            if (search != null && !search.isEmpty()) {
                                return path.getFileName().toString().toLowerCase()
                                    .contains(search.toLowerCase());
                            }
                            
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
            }
        } catch (IOException e) {
            log.error("Dosya sayma hatası", e);
            throw new FileStorageException("Dosyaları sayarken hata oluştu", e);
        }
    }
    
    /**
     * Dosya sıralama için sıralayıcı oluştur
     * @param sort Sıralama kriterleri
     * @return Dosya yolu sıralayıcısı
     */
    private Comparator<Path> getComparator(org.springframework.data.domain.Sort sort) {
        if (sort.isUnsorted()) {
            // Varsayılan olarak oluşturma tarihine göre azalan sıralama
            return (p1, p2) -> {
                try {
                    BasicFileAttributes attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                    BasicFileAttributes attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    return attr2.creationTime().compareTo(attr1.creationTime());
                } catch (IOException e) {
                    return 0;
                }
            };
        }
        
        // Dosya özelliklerine göre sıralama
        return (p1, p2) -> {
            try {
                for (org.springframework.data.domain.Sort.Order order : sort) {
                    int result = 0;
                    
                    switch (order.getProperty()) {
                        case "filename":
                            result = p1.getFileName().toString().compareTo(p2.getFileName().toString());
                            break;
                        case "size":
                            result = Long.compare(Files.size(p1), Files.size(p2));
                            break;
                        case "uploadDate":
                            BasicFileAttributes attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                            BasicFileAttributes attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                            result = attr1.creationTime().compareTo(attr2.creationTime());
                            break;
                        default:
                            continue;
                    }
                    
                    return order.isAscending() ? result : -result;
                }
                
                return 0;
            } catch (IOException e) {
                return 0;
            }
        };
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
    
    /**
     * Dosya türünü (MIME türü) alma
     * @param path Dosya yolu
     * @return MIME türü
     */
    private String getContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            // Fallback: Uzantıya göre basit MIME türü tahmini
            String extension = getFileExtension(path.getFileName().toString()).toLowerCase();
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
                default:
                    return "application/octet-stream";
            }
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