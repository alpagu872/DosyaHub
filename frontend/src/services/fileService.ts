import axios from 'axios';
import api from './api';
import { FileMetadata, FileListResponse, FileSearchParams, FileUploadResponse } from '../types/file';

// API URL tanımlaması - doğru şekilde tanımlanmalı!
const API_URL = 'http://localhost:8080/api';

export const fileService = {
    async uploadFile(file: File, onUploadProgress?: (progressEvent: any) => void): Promise<FileUploadResponse> {
        const formData = new FormData();
        formData.append('file', file);

        // Upload progress için özel bir axios instance kullanıyoruz
        // Ancak token'ı eklemek için manuel olarak Authorization header ekliyoruz
        const token = localStorage.getItem('token');
        console.log('Upload için token:', token ? 'Var' : 'Yok'); // Hata ayıklama için
        
        const response = await axios.post(`${API_URL}/files/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
                'Authorization': token ? `Bearer ${token}` : ''
            },
            onUploadProgress
        });

        console.log('Upload yanıtı:', response.data); // Hata ayıklama için
        return response.data;
    },

    async getFiles(params?: FileSearchParams): Promise<FileListResponse> {
        console.log('getFiles çağrıldı, params:', params); // Hata ayıklama için
        const response = await api.get('/files', { params });
        console.log('getFiles yanıtı:', response.data); // Hata ayıklama için
        return response.data;
    },

    async getFileById(fileId: string): Promise<FileMetadata> {
        const response = await api.get(`/files/${fileId}`);
        return response.data;
    },

    async deleteFile(fileName: string): Promise<any> {
        console.log('deleteFile çağrıldı, fileName:', fileName); // Hata ayıklama için
        
        try {
            // CORS sorunlarını önlemek için, uzun URL'leri kullanmak yerine,
            // dosya adını request body'de gönderelim
            // Bu yaklaşım DELETE isteklerinde oluşan CORS sorunlarını çözecektir
            
            // Dosya adını kısaltmadan API'ye gönderelim
            console.log('Silme isteği gönderiliyor, fileName:', fileName);
            
            // PUT metodu kullanarak dosya adını gövdede gönderelim
            const response = await api.put('/files/delete', { fileName: fileName });
            console.log('deleteFile yanıtı:', response.data);
            return response.data;
        } catch (error) {
            console.error('Dosya silme hatası:', error);
            throw error;
        }
    },

    async shareFile(fileId: string, isPublic: boolean): Promise<FileMetadata> {
        const response = await api.put(`/files/${fileId}/share`, { isPublic });
        return response.data;
    },

    getDownloadUrl(fileName: string): string {
        // Token'ı URL'ye dahil etmek yerine downloadFile metodunu kullanın
        // URL encode edilmesi gereken karakterleri kontrol et ve düzelt
        const encodedFileName = encodeURIComponent(fileName);
        return `${API_URL}/files/download/${encodedFileName}`;
    },

    async downloadFile(fileName: string): Promise<Blob> {
        console.log('downloadFile çağrıldı, fileName:', fileName); // Hata ayıklama için
        
        try {
            // CORS sorunlarını önlemek için, uzun URL'leri kullanmak yerine,
            // dosya adını request body'de gönderelim
            // Bu yaklaşım GET isteklerinde oluşan CORS sorunlarını çözecektir
            
            // Dosya adını kısaltmadan API'ye gönderelim
            console.log('İndirme isteği gönderiliyor, fileName:', fileName);
            
            // POST metodu kullanarak dosya adını gövdede gönderelim
            const response = await api.post('/files/download', 
                { fileName: fileName },
                { responseType: 'blob' }
            );
            
            return response.data;
        } catch (error) {
            console.error('Dosya indirme hatası:', error);
            throw error;
        }
    },

    async renameFile(fileId: string, newName: string): Promise<FileMetadata> {
        const response = await api.put(`/files/${fileId}/rename`, { newName });
        return response.data;
    }
}; 