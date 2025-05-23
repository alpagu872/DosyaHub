import { User } from '../types/auth';
import api from './api';

export const userService = {
    async getCurrentUser(): Promise<User> {
        const response = await api.get('/users/me');
        return response.data;
    },

    async updateProfile(userData: Partial<User>): Promise<User> {
        const response = await api.put('/users/me', userData);
        return response.data;
    },

    async changePassword(data: { currentPassword: string; newPassword: string }): Promise<void> {
        await api.post('/users/me/change-password', data);
    }
}; 