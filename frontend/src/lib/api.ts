const API_BASE_URL = 'http://localhost:8080/api';

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
  startDate: string; // ISO string of LocalDateTime
  endDate: string;   // ISO string of LocalDateTime
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
 * Checks if the token exists, is properly formatted, and not expired.
 * @returns Promise<boolean> True if token is valid, false otherwise.
 */
export const validateToken = async (): Promise<boolean> => {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  try {
    // Try to decode token first to check expiration
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
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });
    
    if (response.status === 401 || response.status === 403) {
      // Token is invalid or expired
      console.log(`Token invalid (${response.status}), clearing auth`);
      authApi.logout();
      
      // If we're on the client side and not on a public frontend route, redirect to login
      if (typeof window !== 'undefined') {
        // Public frontend routes that don't require authentication
        const publicFrontendRoutes = ['/auth', '/login', '/register', '/', '/forgot-password', '/search','/dashboard'];
        const currentPath = window.location.pathname;

        // Only redirect if the user is not already on a public frontend route
        const isPublicFrontendRoute = publicFrontendRoutes.some(
          route => currentPath === route || currentPath.startsWith(route + '/')
        );

        if (!isPublicFrontendRoute) {
          console.log('Redirecting to /auth from:', currentPath);
          window.location.href = '/auth'; // Frontend login page
        } else {
          // If already trying to access a public route (like /auth), don't redirect to avoid loop
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
      throw new ApiError(
        `API request failed: ${response.statusText}`,
        response.status,
        response.statusText
      );
    }
    
    return response.json();
    
  } catch (error) {
    console.error('API Request Error:', error);
    throw error;
  }
}

// Bookings API (for Dashboard)
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

// Authentication API (for login/logout)
export const authApi = {
  login: async (email: string, password: string) => {
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
    
    // DEBUG: Log what we received
    console.log('Login response data:', data);
    
    // Save the token to localStorage
    if (data.token) {
      localStorage.setItem('token', data.token);
      console.log('Token saved to localStorage');
    } else {
      console.warn('No token in login response!', data);
    }
    
    return data;
  },
  
  logout: () => {
    localStorage.removeItem('token');
    console.log('Token removed from localStorage');
  },
  
  getCurrentUser: async () => {
    return apiRequest<UserBasicDTO>('/auth/me');
  },
};

// Boat API (to maintain compatibility with Search page)
export const boatApi = {
  // public
  getAllBoats: async () => {
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
  
  // New method using apiRequest (with authentication if necessary)
  getBoatById: async (boatId: number) => {
    return apiRequest(`/boats/${boatId}`);
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

  // Validate the token (check expiration)
  const isValid = await validateToken();
  if (!isValid) {
    authApi.logout();
    router.push('/auth');
    return false;
  }

  return true;
};