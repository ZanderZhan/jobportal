import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, setAccessToken, setRefreshToken } from '../lib/api';

interface User {
  id: string;
  email: string;
  name: string;
  role: string;
}

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  loginWithGoogle: () => void;
  loginWithMicrosoft: () => void;
  handleOAuthCallback: (provider: 'google' | 'microsoft', code: string, state: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUser = useCallback(async () => {
    try {
      const token = sessionStorage.getItem('access_token');
      if (token) {
        setAccessToken(token);
        const { data } = await api.get('/api/auth/me');
        setUser(data);
      }
    } catch {
      setAccessToken(null);
      setRefreshToken(null);
      sessionStorage.removeItem('access_token');
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  const login = async (email: string, password: string) => {
    const { data } = await api.post('/api/auth/login', { email, password });
    setAccessToken(data.accessToken);
    sessionStorage.setItem('access_token', data.accessToken);
    if (data.refreshToken) {
      setRefreshToken(data.refreshToken);
    }
    setUser(data.user);
  };

  const register = async (email: string, password: string, name: string) => {
    await api.post('/api/auth/register', { email, password, name });
  };

  const getOAuthUrl = async (provider: 'google' | 'microsoft') => {
    const { data } = await api.get(`/api/auth/${provider}/authorize`, {
      params: { redirectUri: '/auth/callback' },
    });
    return data.url as string;
  };

  const loginWithGoogle = () => {
    getOAuthUrl('google').then((url) => {
      window.location.href = url;
    });
  };

  const loginWithMicrosoft = () => {
    getOAuthUrl('microsoft').then((url) => {
      window.location.href = url;
    });
  };

  const handleOAuthCallback = async (provider: 'google' | 'microsoft', code: string, state: string) => {
    const { data } = await api.post(`/api/auth/${provider}/callback`, {
      code,
      state,
      redirectUri: `${window.location.origin}/auth/callback`,
    });
    setAccessToken(data.accessToken);
    sessionStorage.setItem('access_token', data.accessToken);
    if (data.refreshToken) {
      setRefreshToken(data.refreshToken);
    }
    setUser(data.user);
  };

  const logout = async () => {
    const accessToken = sessionStorage.getItem('access_token');
    const refreshToken = sessionStorage.getItem('refresh_token');
    try {
      await api.post(
        '/api/auth/logout',
        {},
        {
          withCredentials: true,
          headers: {
            ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
            ...(refreshToken ? { 'X-Refresh-Token': refreshToken } : {}),
          },
        }
      );
    } finally {
      setAccessToken(null);
      setRefreshToken(null);
      sessionStorage.removeItem('access_token');
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        loginWithGoogle,
        loginWithMicrosoft,
        handleOAuthCallback,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
