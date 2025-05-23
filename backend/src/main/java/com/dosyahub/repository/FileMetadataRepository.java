package com.dosyahub.repository;

import com.dosyahub.model.FileMetadata;
import com.dosyahub.model.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    
    /**
     * Kullanıcıya ait dosyaları bulma
     * @param userId Kullanıcı ID
     * @return Dosya listesi
     */
    List<FileMetadata> findByUserId(UUID userId);
    
    /**
     * Kullanıcıya ait dosyaları sayfalı olarak bulma
     * @param userId Kullanıcı ID
     * @param pageable Sayfalama 
     * @return Dosya sayfası
     */
    Page<FileMetadata> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Kullanıcıya ait belirli bir dosya tipindeki dosyaları bulma
     * @param userId Kullanıcı ID
     * @param fileType Dosya tipi
     * @param pageable Sayfalama
     * @return Dosya sayfası
     */
    Page<FileMetadata> findByUserIdAndFileType(UUID userId, FileType fileType, Pageable pageable);
    
    /**
     * Kullanıcıya ait bir dosyayı ID ile bulma
     * @param id Dosya ID
     * @param userId Kullanıcı ID
     * @return Dosya (varsa)
     */
    Optional<FileMetadata> findByIdAndUserId(UUID id, UUID userId);
    
    /**
     * Kullanıcıya ait dosya sayısını bulma
     * @param userId Kullanıcı ID
     * @return Dosya sayısı
     */
    long countByUserId(UUID userId);
    
    /**
     * Dosya sistemindeki adıyla bir dosyayı bulma
     * @param storedFilename Saklanan dosya adı
     * @return Dosya (varsa)
     */
    Optional<FileMetadata> findByStoredFilename(String storedFilename);
    
    /**
     * Dosya sistemindeki adının bir kısmını içeren dosyaları bulma
     * @param storedFilename Saklanan dosya adının bir kısmı
     * @return Bulunan dosyalar
     */
    List<FileMetadata> findByStoredFilenameContaining(String storedFilename);
} 