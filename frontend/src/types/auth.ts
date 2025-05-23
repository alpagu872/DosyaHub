export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
}

export interface AuthState {
    user: User | null;
    token: string | null;
    isLoading: boolean;
    error: string | null;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
}

export interface AuthResponse {
    token: string;
    userId: string;
    email: string;
    firstName: string;
    lastName: string;
} 