import { LoginRequest, RegisterRequest, AuthResponse, User } from '../types/auth';
import api from './api';

export const authService = {
    async login(data: LoginRequest): Promise<AuthResponse> {
        const response = await api.post('/auth/login', data);
        return response.data;
    },

    async register(data: RegisterRequest): Promise<AuthResponse> {
        const response = await api.post('/auth/register', data);
        return response.data;
    },

    logout(): void {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },

    getToken(): string | null {
        return localStorage.getItem('token');
    },

    setToken(token: string): void {
        localStorage.setItem('token', token);
    },

    getUser(): User | null {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },

    setUser(user: User): void {
        localStorage.setItem('user', JSON.stringify(user));
    },

    isAuthenticated(): boolean {
        return !!this.getToken() && !!this.getUser();
    }
}; 