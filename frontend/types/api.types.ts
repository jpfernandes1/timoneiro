// Based on UserResponseDTO
export interface User {
  id: number;
  name: string;
  phone: string;
  email: string;
}

// Based on BoatResponseDTO
export interface Boat {
  id: number;
  name: string;
  description?: string;
  type: string;
  capacity: number;
  pricePerHour: string;
  photoUrl: string;
  city: string;
  state: string;
  ownerName: string;

}

// For registry endpoint - UserRequestDTO
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  cpf: string;
  phone: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  name: string;
}