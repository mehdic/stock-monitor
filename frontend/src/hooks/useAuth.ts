import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  email: string | null;
  login: (token: string, email: string) => void;
  logout: () => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      isAuthenticated: false,
      token: null,
      email: null,
      login: (token: string, email: string) => {
        localStorage.setItem('jwt_token', token);
        set({ isAuthenticated: true, token, email });
      },
      logout: () => {
        localStorage.removeItem('jwt_token');
        set({ isAuthenticated: false, token: null, email: null });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
