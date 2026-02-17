import api from './api';
import type { 
  ApiResponse, 
  LoginRequest, 
  LoginResponse, 
  RefreshResponse,
  ChangePasswordRequest,
  User 
} from '@/types';

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<ApiResponse<LoginResponse>>('/v1/auth/login', data);
    return response.data.data;
  },

  refresh: async (refreshToken: string): Promise<RefreshResponse> => {
    const response = await api.post<ApiResponse<RefreshResponse>>('/v1/auth/refresh', {
      refreshToken,
    });
    return response.data.data;
  },

  logout: async (refreshToken: string): Promise<void> => {
    await api.post('/v1/auth/logout', { refreshToken });
  },

  getMe: async (): Promise<User> => {
    const response = await api.get<ApiResponse<User>>('/v1/auth/me');
    return response.data.data;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<void> => {
    await api.post('/v1/auth/change-password', data);
  },

  validateToken: async (): Promise<boolean> => {
    try {
      await api.get('/v1/auth/validate');
      return true;
    } catch {
      return false;
    }
  },
};

export type { LoginRequest, LoginResponse };
