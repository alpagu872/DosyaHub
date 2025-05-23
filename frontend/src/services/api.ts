import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// İstek interceptor'u - her istekte token ekler
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Yanıt interceptor'u - 401 hatası kontrolü
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token geçersiz veya süresi dolmuş
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Kullanıcının yönlendirileceği mantığı burada uygulayabilirsiniz
      // Örneğin bir event yayınlayabilirsiniz
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }
    return Promise.reject(error);
  }
);

export default api; 