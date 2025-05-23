import React, { useState, useEffect } from 'react';
import { Container, Typography, Box, Grid, Alert, CircularProgress } from '@mui/material';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../store';
import FileUpload from '../components/FileUpload';
import FileList from '../components/FileList';
import { fetchFiles, uploadFile, deleteFile, clearErrors, clearSuccess } from '../slices/fileSlice';
import { FileUploadResponse } from '../types/file';

const DashboardPage: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { user } = useSelector((state: RootState) => state.auth);
  const { files, isLoading, error, success, totalCount } = useSelector((state: RootState) => state.files);
  
  // Dosya listesi parametreleri
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState('uploadDate,desc');
  const [search, setSearch] = useState('');

  useEffect(() => {
    handleFetchFiles();
  }, [page, size, sort, search]); // Parametreler değiştiğinde dosyaları yeniden yükle

  useEffect(() => {
    // Başarı ve hata mesajlarını 5 saniye sonra otomatik kapat
    let successTimer: NodeJS.Timeout;
    let errorTimer: NodeJS.Timeout;

    if (success) {
      successTimer = setTimeout(() => {
        dispatch(clearSuccess());
      }, 5000);
    }

    if (error) {
      errorTimer = setTimeout(() => {
        dispatch(clearErrors());
      }, 5000);
    }

    return () => {
      clearTimeout(successTimer);
      clearTimeout(errorTimer);
    };
  }, [success, error, dispatch]);

  const handleFetchFiles = async () => {
    dispatch(fetchFiles({ page, size, sort, search }));
  };

  const handleFileUploadSuccess = async (file: File, onProgress?: (progress: number) => void) => {
    try {
      await dispatch(uploadFile({ file, onProgress })).unwrap();
      handleFetchFiles();
    } catch (error) {
      console.error('Dosya yükleme hatası:', error);
    }
  };

  const handleFileDelete = async (fileName: string) => {
    try {
      console.log('Dosya silme işlemi başlatılıyor, dosya adı:', fileName);
      // Eğer dosya adı içinde yol varsa ve bu dosya listesinde varsa devam et
      const fileExists = files.some(file => file.filename === fileName);
      if (!fileExists) {
        console.warn('Silinmeye çalışılan dosya listede bulunamadı:', fileName);
      }
      
      await dispatch(deleteFile(fileName)).unwrap();
      console.log('Dosya silme işlemi başarılı:', fileName);
      // Silme işlemi başarılı olduktan sonra dosya listesini yeniden getir
      handleFetchFiles();
      return Promise.resolve();
    } catch (error) {
      console.error('Dosya silme işlemi başarısız:', error);
      return Promise.reject(error);
    }
  };

  // Sayfalama işleyicisi
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  // Sayfa başına eleman sayısı değiştirme
  const handlePageSizeChange = (newSize: number) => {
    setSize(newSize);
    setPage(0); // Sayfa boyutu değiştiğinde ilk sayfaya dön
  };

  // Sıralama işleyicisi
  const handleSortChange = (newSort: string) => {
    setSort(newSort);
  };

  // Arama işleyicisi
  const handleSearchChange = (newSearch: string) => {
    setSearch(newSearch);
    setPage(0); // Arama değiştiğinde ilk sayfaya dön
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Hoş Geldiniz, {user?.firstName}!
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Dosyalarınızı güvenle yönetin ve paylaşın.
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => dispatch(clearErrors())}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 3 }} onClose={() => dispatch(clearSuccess())}>
          {success}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          {isLoading && files.length === 0 ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : (
            <FileList 
              files={files} 
              onDelete={handleFileDelete} 
              onRefresh={handleFetchFiles} 
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <FileUpload 
            onUploadSuccess={(file: File, onProgress) => 
              handleFileUploadSuccess(file, onProgress)
            } 
          />
        </Grid>
      </Grid>
    </Container>
  );
};

export default DashboardPage; 