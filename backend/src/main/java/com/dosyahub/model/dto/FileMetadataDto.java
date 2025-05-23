package com.dosyahub.model.dto;

import com.dosyahub.model.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataDto {
    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long size;
    private FileType fileType;
    private LocalDateTime uploadedAt;
    private UUID userId;
} 