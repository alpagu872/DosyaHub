import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Avatar,
  Container
} from '@mui/material';
import { Folder } from '@mui/icons-material';
import { logout } from '../slices/authSlice';
import { RootState, AppDispatch } from '../store';

const Navbar: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName?.charAt(0) || ''}${lastName?.charAt(0) || ''}`;
  };

  return (
    <AppBar position="static" sx={{ backgroundColor: '#007bff' }}>
      <Container maxWidth="lg">
        <Toolbar>
          <Box 
            sx={{ 
              display: 'flex', 
              alignItems: 'center', 
              flexGrow: 1, 
              cursor: 'pointer' 
            }}
            onClick={() => navigate('/')}
          >
            <Folder sx={{ fontSize: 32, marginRight: 1, color: '#fff' }} />
            <Typography 
              variant="h5" 
              component="div" 
              sx={{ 
                fontWeight: 'bold', 
                letterSpacing: 0.5,
                display: 'flex',
                alignItems: 'center'
              }}
            >
              Dosya<Typography variant="h5" component="span" sx={{ color: '#ffcc00', fontWeight: 'bold' }}>Hub</Typography>
            </Typography>
          </Box>
          
          {user ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="body1" sx={{ display: { xs: 'none', sm: 'block' } }}>
                {user.firstName} {user.lastName}
              </Typography>
              <Avatar sx={{ bgcolor: '#ffcc00', color: '#333' }}>
                {getInitials(user.firstName, user.lastName)}
              </Avatar>
              <Button 
                color="inherit" 
                onClick={handleLogout}
                sx={{ 
                  bgcolor: 'rgba(255,255,255,0.1)', 
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } 
                }}
              >
                Çıkış Yap
              </Button>
            </Box>
          ) : (
            <Box>
              <Button 
                color="inherit" 
                onClick={() => navigate('/login')}
                sx={{ 
                  mr: 1, 
                  bgcolor: 'rgba(255,255,255,0.1)', 
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' } 
                }}
              >
                Giriş Yap
              </Button>
              <Button 
                variant="contained" 
                color="secondary" 
                onClick={() => navigate('/register')}
                sx={{ bgcolor: '#ffcc00', color: '#333', '&:hover': { bgcolor: '#e6b800' } }}
              >
                Kayıt Ol
              </Button>
            </Box>
          )}
        </Toolbar>
      </Container>
    </AppBar>
  );
};

export default Navbar; 