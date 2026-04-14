import { createContext, useContext, useState, useEffect, useCallback } from 'react';

const BASE = 'http://localhost:8080/api';
const AuthContext = createContext(null);

export async function request(method, path, body) {
  const token = localStorage.getItem('cms_token');
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  };
  const res = await fetch(`${BASE}${path}`, opts);
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Request failed');
  return data.data ?? data.message;
}

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const tok = localStorage.getItem('cms_token');
    if (!tok) { setLoading(false); return; }
    request('GET', '/auth/me')
      .then(setUser)
      .catch(() => localStorage.removeItem('cms_token'))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await request('POST', '/auth/login', { email, password });
    localStorage.setItem('cms_token', data.token);
    setUser(data);
    return data;
  }, []);

  const register = useCallback(async (payload) => {
    const data = await request('POST', '/auth/register', payload);
    localStorage.setItem('cms_token', data.token);
    setUser(data);
    return data;
  }, []);

  const logout = useCallback(async () => {
    await request('POST', '/auth/logout').catch(() => {});
    localStorage.removeItem('cms_token');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
