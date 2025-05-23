import React, { useState, useRef } from 'react';
import { 
  Box, 
  Button, 
  Typography, 
  CircularProgress, 
  LinearProgress, 
  Paper, 
  Alert,
  List,
  ListItem,
  ListItemIcon,
  ListItemText
} from '@mui/material';
import { CloudUpload, PictureAsPdf, Image, Error } from '@mui/icons-material';
import { fileService } from '../services/fileService';
import { FileUploadProgress } from '../types/file';

interface FileUploadProps {
  onUploadSuccess: (file: File, onProgress?: (progress: number) => void) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({ onUploadSuccess }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<FileUploadProgress | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Desteklenen dosya türleri
  const allowedFileTypes = ['application/pdf', 'image/png', 'image/jpeg'];

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      const file = event.target.files[0];
      
      // Dosya türü kontrolü
      if (!allowedFileTypes.includes(file.type)) {
        setUploadError('Desteklenmeyen dosya formatı. Sadece PDF, PNG ve JPG dosyaları yüklenebilir.');
        setSelectedFile(null);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
        return;
      }
      
      setSelectedFile(file);
      setUploadError(null);
    }
  };

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    if (event.dataTransfer.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      
      // Dosya türü kontrolü
      if (!allowedFileTypes.includes(file.type)) {
        setUploadError('Desteklenmeyen dosya formatı. Sadece PDF, PNG ve JPG dosyaları yüklenebilir.');
        setSelectedFile(null);
        return;
      }
      
      setSelectedFile(file);
      setUploadError(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;
    
    try {
      setIsUploading(true);
      setUploadProgress({ loaded: 0, total: 100, progress: 0 });
      
      onUploadSuccess(selectedFile, (progress: number) => {
        setUploadProgress({
          loaded: progress,
          total: 100,
          progress
        });
      });
      
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (error: any) {
      setUploadError(error.message || 'Dosya yükleme hatası');
      console.error('Dosya yükleme hatası:', error);
    } finally {
      setIsUploading(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Dosya Yükle
      </Typography>
      
      {uploadError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {uploadError}
        </Alert>
      )}
      
      <Box sx={{ mb: 2 }}>
        <Alert severity="info" sx={{ mb: 1 }}>
          Desteklenen dosya formatları:
        </Alert>
        <List dense>
          <ListItem>
            <ListItemIcon>
              <PictureAsPdf color="error" />
            </ListItemIcon>
            <ListItemText primary="PDF dosyaları" />
          </ListItem>
          <ListItem>
            <ListItemIcon>
              <Image color="primary" />
            </ListItemIcon>
            <ListItemText primary="PNG resim dosyaları" />
          </ListItem>
          <ListItem>
            <ListItemIcon>
              <Image color="secondary" />
            </ListItemIcon>
            <ListItemText primary="JPG/JPEG resim dosyaları" />
          </ListItem>
        </List>
      </Box>
      
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          border: '2px dashed #ccc',
          borderRadius: 2,
          p: 3,
          mb: 2,
          minHeight: '200px',
          transition: 'all 0.3s',
          bgcolor: 'background.default',
          '&:hover': {
            borderColor: 'primary.main',
            bgcolor: 'rgba(25, 118, 210, 0.04)',
          },
        }}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
      >
        <input
          type="file"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
          ref={fileInputRef}
          accept=".pdf,.png,.jpg,.jpeg"
        />
        
        <CloudUpload fontSize="large" color="primary" sx={{ mb: 2 }} />
        
        {!selectedFile ? (
          <>
            <Typography variant="body1" sx={{ mb: 2, textAlign: 'center' }}>
              Dosyanızı buraya sürükleyip bırakın veya seçmek için tıklayın
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2, textAlign: 'center' }}>
              (Sadece PDF, PNG ve JPG dosyaları)
            </Typography>
            <Button
              variant="contained"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
            >
              Dosya Seç
            </Button>
          </>
        ) : (
          <>
            <Typography variant="body1" sx={{ mb: 1 }}>
              <strong>Seçilen Dosya:</strong> {selectedFile.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {formatFileSize(selectedFile.size)}
            </Typography>
            
            {isUploading ? (
              <Box sx={{ width: '100%', mb: 2 }}>
                <LinearProgress 
                  variant="determinate" 
                  value={uploadProgress?.progress || 0} 
                  sx={{ mb: 1 }}
                />
                <Typography variant="body2" color="text.secondary" align="center">
                  {uploadProgress?.progress}% tamamlandı
                </Typography>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={handleUpload}
                  disabled={isUploading}
                >
                  {isUploading ? (
                    <>
                      <CircularProgress size={24} sx={{ mr: 1 }} /> Yükleniyor...
                    </>
                  ) : (
                    'Yükle'
                  )}
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => {
                    setSelectedFile(null);
                    if (fileInputRef.current) {
                      fileInputRef.current.value = '';
                    }
                  }}
                  disabled={isUploading}
                >
                  İptal
                </Button>
              </Box>
            )}
          </>
        )}
      </Box>
    </Paper>
  );
};

export default FileUpload; 