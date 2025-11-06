import { apiClient } from './api';

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  userId: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  role: string;
}

/**
 * Authentication service handling user registration, login, and logout.
 */
class AuthService {
  /**
   * Register a new user.
   *
   * @param data Registration data (email, password, firstName, lastName)
   * @returns Created user
   */
  async register(data: RegisterData): Promise<User> {
    const response = await apiClient.post<User>('/api/auth/register', data);
    return response.data;
  }

  /**
   * Login with email and password.
   *
   * @param data Login credentials
   * @returns Login response with JWT token
   */
  async login(data: LoginData): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', data);
    return response.data;
  }

  /**
   * Logout current user.
   * Clears local storage and JWT token.
   */
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    localStorage.removeItem('userId');
    localStorage.removeItem('portfolioId');
  }

  /**
   * Verify email with token from verification link.
   *
   * @param token Verification token from email
   */
  async verifyEmail(token: string): Promise<void> {
    await apiClient.post('/api/auth/verify-email', { token });
  }

  /**
   * Request password reset email.
   *
   * @param email User's email address
   */
  async requestPasswordReset(email: string): Promise<void> {
    await apiClient.post('/api/auth/password-reset/request', { email });
  }

  /**
   * Reset password with token from reset email.
   *
   * @param token Reset token from email
   * @param newPassword New password
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await apiClient.post('/api/auth/password-reset/confirm', { token, newPassword });
  }

  /**
   * Get current user profile.
   *
   * @returns User profile
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/api/auth/me');
    return response.data;
  }

  /**
   * Check if user is authenticated.
   *
   * @returns True if JWT token exists in localStorage
   */
  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  /**
   * Get stored JWT token.
   *
   * @returns JWT token or null
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Get stored user email.
   *
   * @returns User email or null
   */
  getEmail(): string | null {
    return localStorage.getItem('email');
  }

  /**
   * Get stored user ID.
   *
   * @returns User ID or null
   */
  getUserId(): string | null {
    return localStorage.getItem('userId');
  }
}

export const authService = new AuthService();
