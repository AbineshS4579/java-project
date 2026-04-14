// src/App.jsx
import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage    from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import UserDashboard  from './pages/UserDashboard';
import AdminDashboard from './pages/AdminDashboard';
import './styles/global.css';

function Router() {
  const { user, loading } = useAuth();
  const [page, setPage] = useState('login'); // login | register

  if (loading) return (
    <div className="splash">
      <div className="spinner" />
      <p>Loading…</p>
    </div>
  );

  if (!user) {
    return page === 'login'
      ? <LoginPage    onSwitch={() => setPage('register')} />
      : <RegisterPage onSwitch={() => setPage('login')} />;
  }

  return user.role === 'admin'
    ? <AdminDashboard />
    : <UserDashboard />;
}

export default function App() {
  return (
    <AuthProvider>
      <Router />
    </AuthProvider>
  );
}
