package com.dosyahub.controller;

import com.dosyahub.model.User;
import com.dosyahub.repository.UserRepository;
import com.dosyahub.service.FileStorageService;
import com.dosyahub.service.MinioStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dosya İşlemleri", description = "Dosya yükleme, indirme ve silme işlemleri")
public class FileController {

    private final FileStorageService fileStorageService;
    private final MinioStorageService minioStorageService;
    private final UserRepository userRepository;

    @Value("${storage.type}")
    private String storageType;

    @Value("${minio.enabled}")
    private boolean minioEnabled;
    
    // Yedek olarak sabit bir kullanıcı ID'si (kimlik doğrulama çalışmazsa veya test için)
    private final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /**
     * Kimlik doğrulama ile kullanıcı ID'sini al veya demo ID kullan
     * @return Kimlik doğrulamadan gelen kullanıcı ID'si veya demo ID
     */
    private UUID getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                String email = authentication.getName();
                return userRepository.findByEmail(email)
                        .map(User::getId)
                        .orElse(DEMO_USER_ID);
            }
        } catch (Exception e) {
            log.warn("Kullanıcı kimliği alınamadı, demo ID kullanılıyor", e);
        }
        return DEMO_USER_ID;
    }

    @GetMapping
    @Operation(
            summary = "Dosyaları Listele",
            description = "Kullanıcıya ait dosyaları listeler, sayfalama ve sıralama desteği ile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosyalar başarıyla listelendi"),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<Map<String, Object>> getFiles(
            @Parameter(description = "Sayfa numarası (0'dan başlar)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa başına öğe sayısı") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sıralama kriteri (örn: uploadDate,desc)") @RequestParam(required = false) String sort,
            @Parameter(description = "Arama terimi") @RequestParam(required = false) String search) {
        
        try {
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya listeleme isteği kullanıcı ID: {}", userId);
            
            // Sıralama parametresini işle
            Sort sortBy = Sort.unsorted();
            if (sort != null && !sort.isEmpty()) {
                String[] sortParams = sort.split(",");
                String sortField = sortParams[0];
                String sortDirection = sortParams.length > 1 ? sortParams[1] : "asc";
                
                sortBy = Sort.by(sortDirection.equalsIgnoreCase("desc") ? 
                        Sort.Direction.DESC : Sort.Direction.ASC, sortField);
            } else {
                // Varsayılan olarak yükleme tarihine göre azalan sırada sırala
                sortBy = Sort.by(Sort.Direction.DESC, "uploadDate");
            }
            
            Pageable pageable = PageRequest.of(page, size, sortBy);
            
            // Dosya listesini getir (burada gerçek implementasyon servisteki metoda bağlı olacak)
            List<Map<String, Object>> files;
            long totalCount = 0;
            
            if (minioEnabled && "minio".equals(storageType)) {
                // MinIO için implementasyon (örnek olarak gösteriyorum, gerçek metoda bağlı)
                files = minioStorageService.listFiles(userId, pageable, search);
                totalCount = minioStorageService.countFiles(userId, search);
            } else {
                // Dosya sistemi için implementasyon
                files = fileStorageService.listFiles(userId, pageable, search);
                totalCount = fileStorageService.countFiles(userId, search);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("totalCount", totalCount);
            response.put("currentPage", page);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Dosya listeleme hatası", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Dosya Yükle",
            description = "Sisteme yeni bir dosya yükler (Sadece PDF, PNG ve JPG dosyaları desteklenir)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosya başarıyla yüklendi"),
                    @ApiResponse(responseCode = "400", description = "Geçersiz istek veya desteklenmeyen dosya formatı", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "Yüklenecek dosya") @RequestParam("file") MultipartFile file) {
        try {
            // Dosya formatı kontrolü - Sadece PDF, PNG ve JPG dosyaları desteklenir
            String contentType = file.getContentType();
            if (contentType == null || !(contentType.equals("application/pdf") || 
                                        contentType.equals("image/png") || 
                                        contentType.equals("image/jpeg"))) {
                
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Desteklenmeyen dosya formatı. Sadece PDF, PNG ve JPG dosyaları yüklenebilir.");
                response.put("status", "error");
                
                return ResponseEntity.badRequest().body(response);
            }
            
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya yükleme isteği kullanıcı ID: {}", userId);
            
            String storedFileName;
            if (minioEnabled && "minio".equals(storageType)) {
                storedFileName = minioStorageService.storeFile(userId, file);
                log.info("Dosya MinIO'ya yüklendi: {}", storedFileName);
            } else {
                storedFileName = fileStorageService.storeFile(userId, file);
                log.info("Dosya dosya sistemine yüklendi: {}", storedFileName);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", storedFileName);
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Dosya yükleme hatası", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(
            summary = "Dosya İndir",
            description = "Belirtilen dosyayı indirir",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosya başarıyla indirildi"),
                    @ApiResponse(responseCode = "404", description = "Dosya bulunamadı", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "İndirilecek dosya adı") @PathVariable String fileName) {
        try {
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya indirme isteği kullanıcı ID: {}, dosya adı: {}", userId, fileName);
            
            InputStream fileStream;
            if (minioEnabled && "minio".equals(storageType)) {
                fileStream = minioStorageService.getFileAsStream(fileName);
            } else {
                fileStream = fileStorageService.getFileAsStream(userId, fileName);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            log.error("Dosya indirme hatası: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/download")
    @Operation(
            summary = "Dosya İndir (POST)",
            description = "Belirtilen dosyayı indirir (Request Body ile)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosya başarıyla indirildi"),
                    @ApiResponse(responseCode = "404", description = "Dosya bulunamadı", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<InputStreamResource> downloadFileWithRequestBody(
            @Parameter(description = "İndirilecek dosya bilgisi") @RequestBody Map<String, String> requestBody) {
        try {
            String fileName = requestBody.get("fileName");
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("Dosya adı gereklidir");
            }
            
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya indirme isteği (POST) kullanıcı ID: {}, dosya adı: {}", userId, fileName);
            
            InputStream fileStream;
            if (minioEnabled && "minio".equals(storageType)) {
                fileStream = minioStorageService.getFileAsStream(fileName);
            } else {
                fileStream = fileStorageService.getFileAsStream(userId, fileName);
            }
            
            // Dosya adından / karakterlerini temizleyerek attachment adını oluştur
            String safeFileName = fileName;
            if (fileName.contains("/")) {
                safeFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            
            // Türkçe karakterler içeren dosya adını düzeltmek için
            String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20"); // URL kodlamasında boşluklar + işaretine dönüşür, bunu düzeltiyoruz

            // RFC 5987 formatında dosya adını ayarlıyoruz
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            log.error("Dosya indirme hatası (POST): {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{fileName}")
    @Operation(
            summary = "Dosya Sil",
            description = "Belirtilen dosyayı siler",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosya başarıyla silindi"),
                    @ApiResponse(responseCode = "404", description = "Dosya bulunamadı", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<Map<String, Object>> deleteFile(
            @Parameter(description = "Silinecek dosya adı") @PathVariable String fileName) {
        try {
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya silme isteği kullanıcı ID: {}, dosya adı: {}", userId, fileName);
            
            if (minioEnabled && "minio".equals(storageType)) {
                minioStorageService.deleteFile(fileName);
            } else {
                fileStorageService.deleteFile(userId, fileName);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("status", "deleted");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Dosya silme hatası: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping("/delete")
    @Operation(
            summary = "Dosya Sil (PUT)",
            description = "Belirtilen dosyayı siler (Request Body ile)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dosya başarıyla silindi"),
                    @ApiResponse(responseCode = "404", description = "Dosya bulunamadı", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Sunucu hatası", content = @Content)
            }
    )
    public ResponseEntity<Map<String, Object>> deleteFileWithRequestBody(
            @Parameter(description = "Silinecek dosya bilgisi") @RequestBody Map<String, String> requestBody) {
        try {
            String fileName = requestBody.get("fileName");
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("Dosya adı gereklidir");
            }
            
            // Kimlik doğrulamadan kullanıcı ID'sini al
            UUID userId = getCurrentUserId();
            log.info("Dosya silme isteği (PUT) kullanıcı ID: {}, dosya adı: {}", userId, fileName);
            
            if (minioEnabled && "minio".equals(storageType)) {
                minioStorageService.deleteFile(fileName);
            } else {
                fileStorageService.deleteFile(userId, fileName);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("status", "deleted");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Dosya silme hatası (PUT): {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 