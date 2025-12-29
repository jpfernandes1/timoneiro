const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Backend-based types
export interface Address {
  id?: number;
  street: string;
  number: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  marina?: string;
}

export interface BoatBasicDTO {
  id: number;
  name: string;
  type: string;
  address: Address;
  photos?: string[];
}

export interface UserBasicDTO {
  id: number;
  name: string;
  email: string;
}

export interface BookingResponse {
  id: number;
  user: UserBasicDTO;
  boat: BoatBasicDTO;
  startDate: string;
  endDate: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FINISHED';
  totalPrice: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

// Interface para resposta de barcos do usuário
export interface BoatResponseDTO {
  id: number;
  name: string;
  description: string;
  type: string;
  capacity: number;
  length: number | null;
  speed: number | null;
  fabrication: number | null;
  amenities: string[];
  photos: string[]; // VEM ORDENADO PELO MAPPER!
  pricePerHour: number;
  city: string;
  state: string;
  marina: string;
  ownerName: string;
  ownerId: number;
}

// Custom error class
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public statusText: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Validates the current JWT token stored in localStorage.
 */
export const validateToken = async (): Promise<boolean> => {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return false;
    }
    
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    const isExpired = payload.exp * 1000 < Date.now();
    
    if (isExpired) {
      console.log('Token expired, logging out');
      authApi.logout();
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('Error validating token:', error);
    return false;
  }
};

// Basic function for requests
async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('token');
  
  const headers = new Headers();
  headers.append('Content-Type', 'application/json');

  if (token) {
    headers.append('Authorization', `Bearer ${token}`);
  }

  try {
    // CORREÇÃO: Remove /api duplicado do endpoint
    const url = endpoint.startsWith('/') 
      ? `${API_BASE_URL}${endpoint}`
      : `${API_BASE_URL}/${endpoint}`;
    
    const response = await fetch(url, {
      ...options,
      headers,
    });
    
    if (response.status === 401 || response.status === 403) {
      // Token is invalid or expired
      console.log(`Token invalid (${response.status}), clearing auth`);
      authApi.logout();
      
      if (typeof window !== 'undefined') {
        const publicFrontendRoutes = ['/auth', '/login', '/register', '/', '/forgot-password', '/search'];
        const currentPath = window.location.pathname;

        const isPublicFrontendRoute = publicFrontendRoutes.some(
          route => currentPath === route || currentPath.startsWith(route + '/')
        );

        if (!isPublicFrontendRoute) {
          console.log('Redirecting to /auth from:', currentPath);
          window.location.href = '/auth';
        } else {
          console.log('Already on a public frontend route:', currentPath);
        }
      }
      
      throw new ApiError(
        'Authentication failed. Please login again.',
        response.status,
        response.statusText
      );
    }
    
    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch {
        errorData = { message: response.statusText };
      }
      
      throw new ApiError(
        errorData.message || `API request failed: ${response.statusText}`,
        response.status,
        response.statusText
      );
    }
    
    // Se for resposta sem conteúdo (204 No Content)
    if (response.status === 204) {
      return null as T;
    }
    
    return response.json();
    
  } catch (error) {
    console.error('API Request Error:', error);
    throw error;
  }
}

// Bookings API
export const bookingApi = {
  // Search user bookings
  getMyBookings: async (
    page: number = 0,
    size: number = 20,
    status?: string
  ) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...(status && { status }),
    });
    
    return apiRequest<PaginatedResponse<BookingResponse>>(`/api/bookings/my-bookings?${params}`);
  },
  
  // booking details
  getBookingById: async (bookingId: number) => {
    return apiRequest<BookingResponse>(`/bookings/${bookingId}`);
  },
  
  // Cancel booking
  cancelBooking: async (bookingId: number) => {
    return apiRequest<void>(`/bookings/${bookingId}/cancel`, {
      method: 'POST',
    });
  },
};

// Authentication API
export const authApi = {
  login: async (email: string, password: string) => {
    // CORREÇÃO: Usar API_BASE_URL corretamente
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Login failed: ${response.status} - ${errorText}`);
    }
    
    const data = await response.json();
    
    if (data.token) {
      localStorage.setItem('token', data.token);
    } else {
      console.warn('No token in login response!', data);
    }
    
    return data;
  },
  
  logout: () => {
    localStorage.removeItem('token');
  },
  
  getCurrentUser: async () => {
    return apiRequest<UserBasicDTO>('/auth/my-boats');
  },
};

// Boat API
export const boatApi = {
  // Public endpoint (sem autenticação)
  getAllBoats: async (): Promise<BoatResponseDTO[]> => {
    const response = await fetch(`${API_BASE_URL}/boats`, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new Error(`Failed to fetch boats: ${response.status} - ${response.statusText}`);
    }
    
    return response.json();
  },
  
  // Get boat by ID (pode requerer autenticação dependendo da regra)
  getBoatById: async (boatId: number): Promise<BoatResponseDTO> => {
    return apiRequest<BoatResponseDTO>(`/boats/${boatId}`);
  },

  // Get logged user boats (REQUER AUTENTICAÇÃO)
  getMyBoats: async (page: number = 0, size: number = 10): Promise<PaginatedResponse<BoatResponseDTO>> => {
    return apiRequest<PaginatedResponse<BoatResponseDTO>>(`api/boats/my-boats?page=${page}&size=${size}`);
  },
  
  // Get logged user boats with pagination (se implementado no backend)
  getMyBoatsPaginated: async (page: number = 0, size: number = 10): Promise<PaginatedResponse<BoatResponseDTO>> => {
    return apiRequest<PaginatedResponse<BoatResponseDTO>>(`api/boats/my-boats?page=${page}&size=${size}`);
  },
};

// Helper to verify authentication
export const isAuthenticated = (): boolean => {
  return !!localStorage.getItem('token');
};

// Helper to redirect if not authenticated.
export const requireAuth = async (router: any): Promise<boolean> => {
  if (!isAuthenticated()) {
    router.push('/auth');
    return false;
  }

  const isValid = await validateToken();
  if (!isValid) {
    authApi.logout();
    router.push('/auth');
    return false;
  }

  return true;
};