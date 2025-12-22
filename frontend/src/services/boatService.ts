
import { apiClient } from '@/lib/api-client';
import { Boat, BoatRequestDTO } from '@/types/api.types';

export const boatService = {
  getAllBoats: async (): Promise<Boat[]> => {
    return apiClient.get<Boat[]>('/boats');
  },

  getBoat: async (id: number): Promise<Boat> => {
    return apiClient.get<Boat>(`/boats/${id}`);
  },

  createBoat: async (data: BoatRequestDTO): Promise<Boat> => {
    return apiClient.post<Boat>('/boats', data);
  },

  updateBoat: async (id: number, data: Partial<Boat>): Promise<Boat> => {
    return apiClient.put<Boat>(`/boats/${id}`, data);
  },

  deleteBoat: async (id: number): Promise<void> => {
    return apiClient.delete(`/boats/${id}`);
  },
};