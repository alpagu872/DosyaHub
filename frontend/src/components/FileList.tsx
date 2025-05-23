import React, { useState } from 'react';
import {
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  IconButton,
  Divider,
  Box,
  Menu,
  MenuItem,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Button,
  Chip,
  Tooltip,
  Alert
} from '@mui/material';
import {
  InsertDriveFile,
  Delete,
  GetApp,
  MoreVert,
  Image,
  PictureAsPdf,
  Description,
  Code,
  VideoLibrary,
  AudioFile,
  Archive
} from '@mui/icons-material';
import { fileService } from '../services/fileService';
import { FileMetadata } from '../types/file';

interface FileListProps {
  files: FileMetadata[];
  onDelete: (fileName: string) => Promise<void>;
  onRefresh: () => void;
}

const FileList: React.FC<FileListProps> = ({ files, onDelete, onRefresh }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedFile, setSelectedFile] = useState<FileMetadata | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, file: FileMetadata) => {
    setAnchorEl(event.currentTarget);
    setSelectedFile(file);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleDownload = async () => {
    if (!selectedFile) return;
    
    try {
      setLoading(true);
      handleMenuClose();
      const blob = await fileService.downloadFile(selectedFile.filename);
      
      // Dosyayı indirme işlemi
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = selectedFile.originalName || selectedFile.filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Dosya indirme hatası:', error);
      setError('Dosya indirme sırasında bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = () => {
    handleMenuClose();
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!selectedFile) return;
    
    try {
      setLoading(true);
      await onDelete(selectedFile.filename);
      setDeleteDialogOpen(false);
      onRefresh();
    } catch (error) {
      console.error('Dosya silme hatası:', error);
      setError('Dosya silme sırasında bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
  };

  const getFileIcon = (contentType: string) => {
    if (contentType.startsWith('image/')) return <Image color="primary" />;
    if (contentType.startsWith('application/pdf')) return <PictureAsPdf color="error" />;
    if (contentType.startsWith('text/')) return <Description color="info" />;
    if (contentType.startsWith('application/json') || 
        contentType.includes('javascript') || 
        contentType.includes('xml') || 
        contentType.includes('html')) {
      return <Code color="secondary" />;
    }
    if (contentType.startsWith('video/')) return <VideoLibrary color="success" />;
    if (contentType.startsWith('audio/')) return <AudioFile color="warning" />;
    if (contentType.includes('zip') || 
        contentType.includes('tar') || 
        contentType.includes('rar') || 
        contentType.includes('compressed')) {
      return <Archive color="warning" />;
    }
    return <InsertDriveFile />;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleDateString('tr-TR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <Paper sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" gutterBottom>
        Dosyalarım
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      
      {files.length === 0 ? (
        <Box 
          sx={{ 
            flex: 1, 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center',
            flexDirection: 'column',
            p: 3
          }}
        >
          <InsertDriveFile sx={{ fontSize: 60, color: 'text.disabled', mb: 2 }} />
          <Typography variant="body1" color="text.secondary" align="center">
            Henüz hiç dosya yüklemediniz. Yeni dosya yüklemek için sağdaki bölümü kullanabilirsiniz.
          </Typography>
        </Box>
      ) : (
        <List sx={{ width: '100%', bgcolor: 'background.paper', overflow: 'auto', flex: 1 }}>
          {files.map((file, index) => (
            <React.Fragment key={file.id}>
              {index > 0 && <Divider component="li" />}
              <ListItem alignItems="flex-start">
                <ListItemIcon>
                  {getFileIcon(file.contentType)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography
                      component="span"
                      variant="body1"
                      sx={{ fontWeight: 500, display: 'block', mb: 0.5 }}
                    >
                      {file.originalName || file.filename}
                    </Typography>
                  }
                  secondary={
                    <React.Fragment>
                      <Typography
                        component="span"
                        variant="body2"
                        color="text.secondary"
                      >
                        {formatFileSize(file.size)} · {formatDate(file.uploadDate)}
                      </Typography>
                      <Box component="span" sx={{ display: 'block', mt: 0.5 }}>
                        <Chip 
                          size="small" 
                          label={file.contentType}
                          sx={{ fontSize: '0.7rem' }}
                        />
                        {file.isPublic && (
                          <Chip 
                            size="small" 
                            label="Herkese Açık"
                            color="success"
                            sx={{ ml: 1, fontSize: '0.7rem' }}
                          />
                        )}
                      </Box>
                    </React.Fragment>
                  }
                />
                <ListItemSecondaryAction>
                  <Tooltip title="İndir">
                    <IconButton edge="end" onClick={() => {
                      setSelectedFile(file);
                      handleDownload();
                    }}>
                      <GetApp />
                    </IconButton>
                  </Tooltip>
                  <IconButton edge="end" onClick={(e) => handleMenuOpen(e, file)}>
                    <MoreVert />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            </React.Fragment>
          ))}
        </List>
      )}
      
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleDownload}>
          <ListItemIcon>
            <GetApp fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="İndir" />
        </MenuItem>
        <MenuItem onClick={handleDeleteClick}>
          <ListItemIcon>
            <Delete fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText primary="Sil" />
        </MenuItem>
      </Menu>
      
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Dosyayı Sil</DialogTitle>
        <DialogContent>
          <DialogContentText>
            "{selectedFile?.originalName || selectedFile?.filename}" dosyasını silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={loading}>
            İptal
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" disabled={loading}>
            {loading ? 'Siliniyor...' : 'Sil'}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default FileList; 