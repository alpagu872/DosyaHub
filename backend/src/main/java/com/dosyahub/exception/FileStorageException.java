package com.dosyahub.exception;

/**
 * Dosya depolama işlemleri sırasında oluşabilecek hatalar için özel hata sınıfı
 */
public class FileStorageException extends RuntimeException {
    
    public FileStorageException(String message) {
        super(message);
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
} 