const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

// Helper for building consistent URLs.
const buildUrl = (endpoint: string): string => {
  // Remove duplicate /api if present
  const cleanEndpoint = endpoint.replace(/^\/api/, '');
  // Make sure it starts with /
  const normalizedEndpoint = cleanEndpoint.startsWith('/') ? cleanEndpoint : `/${cleanEndpoint}`;
  return `${API_BASE_URL}${normalizedEndpoint}`;
};

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
  photos: string[];
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
  if (typeof window === 'undefined') return false;
  
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

  if (typeof window === 'undefined') {
    throw new Error('apiRequest can only be called in the browser');
  }
  
  const token = localStorage.getItem('token');
  
  const headers = new Headers();
  headers.append('Content-Type', 'application/json');

  if (token) {
    headers.append('Authorization', `Bearer ${token}`);
  }

  try {
    const url = endpoint.startsWith('/') 
      ? `${API_BASE_URL}${endpoint}`
      : `${API_BASE_URL}/${endpoint}`;
    
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include', 
      mode: 'cors'
    });
    
    if (response.status === 401 || response.status === 403) {
      authApi.logout();
      
      // It only redirects if you are not on a public page.
      const publicFrontendRoutes = ['/auth', '/login', '/register', '/', '/forgot-password', '/search'];
      const currentPath = window.location.pathname;

      const isPublicFrontendRoute = publicFrontendRoutes.some(
        route => currentPath === route || currentPath.startsWith(route + '/')
      );

      if (!isPublicFrontendRoute) {
        console.log('Redirecting to /auth from:', currentPath);
        window.location.href = '/auth';
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
    
    return apiRequest<PaginatedResponse<BookingResponse>>(`/bookings/my-bookings?${params}`);
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
    
    const url = buildUrl('/auth/login');
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
      credentials: 'include', 
      mode: 'cors'
    });
    
    if (!response.ok) {
      let errorText;
      try {
        const errorData = await response.json();
        errorText = errorData.message || JSON.stringify(errorData);
      } catch {
        errorText = await response.text();
      }
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
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
    }
  },
  
  getCurrentUser: async (): Promise<UserBasicDTO> => {
    return apiRequest<UserBasicDTO>('/auth/me');
  },
  
  // Add to check backend health.
  checkBackendHealth: async (): Promise<{ status: string }> => {
    // Remove /api from the base URL to access /actuator/health
    const baseWithoutApi = API_BASE_URL.replace(/\/api$/, '');
    const response = await fetch(`${baseWithoutApi}/actuator/health`, {
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error(`Backend health check failed: ${response.status}`);
    }
    
    return response.json();
  },

  register: async (userData: {
  name: string;
  email: string;
  password: string;
  cpf: string;
  phone: string;
}) => {
  const url = buildUrl('/auth/register');

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(userData),
    credentials: 'include',
    mode: 'cors'
  });

  
  if (!response.ok) {
    let errorText;
    try {
      const errorData = await response.json();
      errorText = errorData.message || JSON.stringify(errorData);
    } catch {
      errorText = await response.text();
    }
    throw new Error(`Registration failed: ${response.status} - ${errorText}`);
  }
  
  return response.json();
},
};

// Boat API
export const boatApi = {
  // Public endpoint (sem autenticação)
  getAllBoats: async (): Promise<BoatResponseDTO[]> => {
    const url = buildUrl('/boats');
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    });
    
    if (!response.ok) {
      throw new Error(`Failed to fetch boats: ${response.status} - ${response.statusText}`);
    }
    
    return response.json();
  },
  
  // Get boat by ID
  getBoatById: async (boatId: number): Promise<BoatResponseDTO> => {
    return apiRequest<BoatResponseDTO>(`/boats/${boatId}`);
  },

  // Get logged user boats
  getMyBoats: async (page: number = 0, size: number = 10): Promise<PaginatedResponse<BoatResponseDTO>> => {
    return apiRequest<PaginatedResponse<BoatResponseDTO>>(`/boats/my-boats?page=${page}&size=${size}`);
  },
  
  searchBoats: async (params?: {
    city?: string;
    state?: string;
    type?: string;
    minPrice?: number;
    maxPrice?: number;
    page?: number;
    size?: number;
  }) => {
    const searchParams = new URLSearchParams();
    
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          searchParams.append(key, value.toString());
        }
      });
    }
    
    const queryString = searchParams.toString();
    return apiRequest<PaginatedResponse<BoatResponseDTO>>(
      `/boats/search${queryString ? `?${queryString}` : ''}`
    );
  },
};

// Helper to verify authentication
export const isAuthenticated = (): boolean => {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem('token');
};

// Helper to redirect if not authenticated
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