import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { fileService } from '../services/fileService';
import { FileMetadata, FileUploadResponse } from '../types/file';

interface FileState {
  files: FileMetadata[];
  totalCount: number;
  isLoading: boolean;
  uploadProgress: number | null;
  error: string | null;
  success: string | null;
}

const initialState: FileState = {
  files: [],
  totalCount: 0,
  isLoading: false,
  uploadProgress: null,
  error: null,
  success: null
};

export const fetchFiles = createAsyncThunk(
  'files/fetchFiles',
  async (params: { page?: number; size?: number; sort?: string; search?: string } = {}, { rejectWithValue }) => {
    try {
      console.log('fetchFiles çağrılıyor:', params);
      const response = await fileService.getFiles(params);
      console.log('fetchFiles yanıtı:', response);
      return response;
    } catch (error: any) {
      console.error('fetchFiles hatası:', error);
      return rejectWithValue(
        error.response?.data?.message || 'Dosyalar yüklenirken bir hata oluştu'
      );
    }
  }
);

export const uploadFile = createAsyncThunk(
  'files/uploadFile',
  async (
    { file, onProgress }: { file: File; onProgress?: (progress: number) => void },
    { rejectWithValue }
  ) => {
    try {
      console.log('uploadFile çağrılıyor:', file.name);
      const response = await fileService.uploadFile(file, (progressEvent) => {
        if (progressEvent.lengthComputable && onProgress) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percentCompleted);
        }
      });
      console.log('uploadFile yanıtı:', response);
      return response;
    } catch (error: any) {
      console.error('uploadFile hatası:', error);
      return rejectWithValue(
        error.response?.data?.message || 'Dosya yüklenirken bir hata oluştu'
      );
    }
  }
);

export const deleteFile = createAsyncThunk(
  'files/deleteFile',
  async (fileName: string, { rejectWithValue }) => {
    try {
      console.log('deleteFile çağrılıyor:', fileName);
      const response = await fileService.deleteFile(fileName);
      console.log('deleteFile yanıtı:', response);
      return fileName;
    } catch (error: any) {
      console.error('deleteFile hatası:', error);
      return rejectWithValue(
        error.response?.data?.message || 'Dosya silinirken bir hata oluştu'
      );
    }
  }
);

const fileSlice = createSlice({
  name: 'files',
  initialState,
  reducers: {
    setUploadProgress: (state, action) => {
      state.uploadProgress = action.payload;
    },
    clearUploadProgress: (state) => {
      state.uploadProgress = null;
    },
    clearErrors: (state) => {
      state.error = null;
    },
    clearSuccess: (state) => {
      state.success = null;
    }
  },
  extraReducers: (builder) => {
    builder
      // Dosya Listesi
      .addCase(fetchFiles.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchFiles.fulfilled, (state, action) => {
        state.isLoading = false;
        console.log('fetchFiles fulfilled:', action.payload);
        state.files = action.payload.files || [];
        state.totalCount = action.payload.totalCount || 0;
      })
      .addCase(fetchFiles.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
        console.error('fetchFiles rejected:', action.payload);
      })
      
      // Dosya Yükleme
      .addCase(uploadFile.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(uploadFile.fulfilled, (state, action) => {
        state.isLoading = false;
        state.uploadProgress = null;
        console.log('uploadFile fulfilled:', action.payload);
        state.success = `${action.payload.filename || 'Dosya'} başarıyla yüklendi`;
      })
      .addCase(uploadFile.rejected, (state, action) => {
        state.isLoading = false;
        state.uploadProgress = null;
        state.error = action.payload as string;
        console.error('uploadFile rejected:', action.payload);
      })
      
      // Dosya Silme
      .addCase(deleteFile.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(deleteFile.fulfilled, (state, action) => {
        state.isLoading = false;
        state.files = state.files.filter(file => file.filename !== action.payload);
        console.log('deleteFile fulfilled:', action.payload);
        state.success = 'Dosya başarıyla silindi';
      })
      .addCase(deleteFile.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
        console.error('deleteFile rejected:', action.payload);
      });
  }
});

export const { 
  setUploadProgress, 
  clearUploadProgress, 
  clearErrors, 
  clearSuccess 
} = fileSlice.actions;

export default fileSlice.reducer; 