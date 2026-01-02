'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { authApi, validateToken, isAuthenticated as checkIsAuthenticated } from '@/src/lib/api';

export interface User {
  id: number;
  email: string;
  name: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  register: (userData: any) => Promise<void>;
  isAuthenticated: boolean;
  updateToken: (newToken: string) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Initialize auth state from localStorage and validate token
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const storedToken = localStorage.getItem('token');
        
        if (storedToken) {
          // Validate the token before setting state
          const isValid = await validateToken();
          
          if (isValid) {
            setToken(storedToken);
            
            try {
              // Get user data from backend
              const userData = await authApi.getCurrentUser();
              const user: User = {
                 id: userData.id,
                 email: userData.email,
                 name: userData.name,
                 role: 'ROLE_USER'
               };

              setUser(user);
              localStorage.setItem('user', JSON.stringify(user));
            } catch (error) {
              console.error('Failed to fetch user data:', error);
              // If we can't get user data, clear token
              logout();
            }
          } else {
            // Token is invalid, clear it
            logout();
          }
        }
      } catch (error) {
        console.error('Error initializing auth:', error);
        logout();
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const updateToken = (newToken: string) => {
    setToken(newToken);
    localStorage.setItem('token', newToken);
  };

  const login = async (email: string, password: string) => {
    try {
      setIsLoading(true);

      console.log('🔐 AuthContext: Attempting login...');

      // Use authApi from lib/api.ts for consistency
      const data = await authApi.login(email, password);
      
      // authApi.login already saves token to localStorage
      setToken(data.token || localStorage.getItem('token'));

      console.log('🔑 Token stored:', token ? 'Yes' : 'No');
      
      // Get user data from backend
      const userData = await authApi.getCurrentUser();
      setUser({
        id: userData.id,
        email: userData.email,
        name: userData.name,
        role: 'ROLE_USER'
      });
      
      // Save minimal user info to localStorage (optional)
      localStorage.setItem('user', JSON.stringify({
        id: userData.id,
        email: userData.email,
        name: userData.name
      }));
      
      // Check for pending redirection
      const redirectAfterLogin = localStorage.getItem('redirectAfterLogin');
      if (redirectAfterLogin) {
        localStorage.removeItem('redirectAfterLogin');
        router.push(redirectAfterLogin);
      } else {
        router.push('/dashboard');
      }
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (userData: any) => {
  try {
    setIsLoading(true);
    
    const data = await authApi.register({
      name: userData.name,
      email: userData.email,
      password: userData.password,
      cpf: userData.cpf?.replace(/\D/g, '') || '',
      phone: userData.phone?.replace(/\D/g, '') || ''
    });
    
    const storedToken = localStorage.getItem('token');
    
    if (storedToken) {
      setToken(storedToken);
      
      try {
        const userDataResponse = await authApi.getCurrentUser();
        const newUser = {
          id: userDataResponse.id,
          email: userDataResponse.email,
          name: userDataResponse.name,
          role: 'ROLE_USER'
        };
        
        setUser(newUser);
        localStorage.setItem('user', JSON.stringify(newUser));
      } catch (userError) {
        console.error('Failed to fetch user data after registration:', userError);

        if (data.userId || data.email || data.name) {
          const fallbackUser = {
            id: data.userId || 0,
            email: data.email || userData.email,
            name: data.name || userData.name,
            role: 'ROLE_USER'
          };
          setUser(fallbackUser);
          localStorage.setItem('user', JSON.stringify(fallbackUser));
        }
      }
      
      const redirectAfterLogin = localStorage.getItem('redirectAfterLogin');
      if (redirectAfterLogin) {
        localStorage.removeItem('redirectAfterLogin');
        router.push(redirectAfterLogin);
      } else {
        router.push('/dashboard');
      }
    } else {

      await login(userData.email, userData.password);
    }
  } catch (error) {
    console.error('Registration failed:', error);
    throw error;
  } finally {
    setIsLoading(false);
  }
};

  const logout = () => {
    // Clear all auth-related data
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    setToken(null);
    setUser(null);
    
    // Call authApi logout for consistency
    authApi.logout();
    
    router.push('/auth');
  };

  const isAuthenticated = !!token && checkIsAuthenticated();

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        login,
        logout,
        register,
        isAuthenticated,
        updateToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};