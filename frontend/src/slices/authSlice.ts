import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { AuthState, LoginRequest, RegisterRequest } from '../types/auth';
import { authService } from '../services/authService';

const initialState: AuthState = {
    user: authService.getUser(),
    token: authService.getToken(),
    isLoading: false,
    error: null
};

export const login = createAsyncThunk(
    'auth/login',
    async (data: LoginRequest, { rejectWithValue }) => {
        try {
            const response = await authService.login(data);
            authService.setToken(response.token);
            authService.setUser({
                id: response.userId,
                email: response.email,
                firstName: response.firstName,
                lastName: response.lastName
            });
            return response;
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Giriş başarısız');
        }
    }
);

export const register = createAsyncThunk(
    'auth/register',
    async (data: RegisterRequest, { rejectWithValue }) => {
        try {
            const response = await authService.register(data);
            authService.setToken(response.token);
            authService.setUser({
                id: response.userId,
                email: response.email,
                firstName: response.firstName,
                lastName: response.lastName
            });
            return response;
        } catch (error: any) {
            return rejectWithValue(error.response?.data?.message || 'Kayıt başarısız');
        }
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        logout: (state) => {
            authService.logout();
            state.user = null;
            state.token = null;
            state.error = null;
        },
        clearError: (state) => {
            state.error = null;
        }
    },
    extraReducers: (builder) => {
        builder
            // Login
            .addCase(login.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(login.fulfilled, (state, action) => {
                state.isLoading = false;
                state.user = {
                    id: action.payload.userId,
                    email: action.payload.email,
                    firstName: action.payload.firstName,
                    lastName: action.payload.lastName
                };
                state.token = action.payload.token;
            })
            .addCase(login.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload as string;
            })
            // Register
            .addCase(register.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(register.fulfilled, (state, action) => {
                state.isLoading = false;
                state.user = {
                    id: action.payload.userId,
                    email: action.payload.email,
                    firstName: action.payload.firstName,
                    lastName: action.payload.lastName
                };
                state.token = action.payload.token;
            })
            .addCase(register.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload as string;
            });
    }
});

export const { logout, clearError } = authSlice.actions;
export default authSlice.reducer; 