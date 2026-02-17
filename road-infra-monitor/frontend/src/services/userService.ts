import api from './api';
import type { 
  ApiResponse, 
  PaginatedResponse,
  User,
  CreateUserRequest,
  UpdateUserRequest,
  ChangeRoleRequest,
  ResetPasswordRequest,
  Role,
} from '@/types';

export interface UserFilter {
  role?: Role;
  enabled?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export interface UserStats {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  lockedUsers: number;
  byRole: Record<Role, number>;
}

export const userService = {
  // List users with pagination and filters
  listUsers: async (filter: UserFilter = {}): Promise<PaginatedResponse<User>> => {
    const params = new URLSearchParams();
    if (filter.role) params.append('role', filter.role);
    if (filter.enabled !== undefined) params.append('enabled', String(filter.enabled));
    if (filter.search) params.append('search', filter.search);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const response = await api.get<ApiResponse<PaginatedResponse<User>>>(
      `/v1/users?${params.toString()}`
    );
    return response.data.data;
  },

  // Get user by ID
  getUser: async (userId: string): Promise<User> => {
    const response = await api.get<ApiResponse<User>>(`/v1/users/${userId}`);
    return response.data.data;
  },

  // Get user by username
  getUserByUsername: async (username: string): Promise<User> => {
    const response = await api.get<ApiResponse<User>>(`/v1/users/username/${username}`);
    return response.data.data;
  },

  // Create new user
  createUser: async (data: CreateUserRequest): Promise<User> => {
    const response = await api.post<ApiResponse<User>>('/v1/users', data);
    return response.data.data;
  },

  // Update user profile
  updateUser: async (userId: string, data: UpdateUserRequest): Promise<User> => {
    const response = await api.put<ApiResponse<User>>(`/v1/users/${userId}`, data);
    return response.data.data;
  },

  // Change user role
  changeRole: async (userId: string, data: ChangeRoleRequest): Promise<User> => {
    const response = await api.put<ApiResponse<User>>(`/v1/users/${userId}/role`, data);
    return response.data.data;
  },

  // Enable user
  enableUser: async (userId: string): Promise<User> => {
    const response = await api.post<ApiResponse<User>>(`/v1/users/${userId}/enable`);
    return response.data.data;
  },

  // Disable user
  disableUser: async (userId: string): Promise<User> => {
    const response = await api.post<ApiResponse<User>>(`/v1/users/${userId}/disable`);
    return response.data.data;
  },

  // Unlock user account
  unlockUser: async (userId: string): Promise<User> => {
    const response = await api.post<ApiResponse<User>>(`/v1/users/${userId}/unlock`);
    return response.data.data;
  },

  // Reset user password
  resetPassword: async (userId: string, data: ResetPasswordRequest): Promise<void> => {
    await api.post(`/v1/users/${userId}/reset-password`, data);
  },

  // Delete user
  deleteUser: async (userId: string): Promise<void> => {
    await api.delete(`/v1/users/${userId}`);
  },

  // Get user statistics
  getUserStats: async (): Promise<UserStats> => {
    const response = await api.get<ApiResponse<UserStats>>('/v1/users/stats');
    return response.data.data;
  },

  // Get users by role
  getUsersByRole: async (role: Role): Promise<User[]> => {
    const response = await api.get<ApiResponse<User[]>>(`/v1/users/role/${role}`);
    return response.data.data;
  },
};
