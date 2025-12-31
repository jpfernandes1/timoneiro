
import { apiClient } from '@/lib/api-client';
import { User, RegisterRequest, AuthResponse } from '@/types/api.types';

export const userService = {
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    return apiClient.post<AuthResponse>('/users/register', data);
  },

  getProfile: async (): Promise<User> => {
    return apiClient.get<User>('/users/profile');
  },

  getAllUsers: async (): Promise<User[]> => {
    return apiClient.get<User[]>('/users');
  },

  updateProfile: async (data: Partial<RegisterRequest>): Promise<User> => {
    return apiClient.put<User>('/users/profile', data);
  },
};