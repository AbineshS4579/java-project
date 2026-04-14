// src/pages/LoginPage.jsx
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage({ onSwitch }) {
  const { login } = useAuth();
  const [form,    setForm]    = useState({ email: '', password: '' });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const handle = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async e => {
    e.preventDefault();
    setError('');
    if (!form.email || !form.password) { setError('All fields required.'); return; }
    setLoading(true);
    try {
      await login(form.email, form.password);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-box">
        <div className="auth-header">
          <div style={{ fontSize: 40, marginBottom: 8 }}>🗂️</div>
          <h1>Welcome back</h1>
          <p>Sign in to manage your complaints</p>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {error && <div className="alert-error">{error}</div>}
          <div className="field">
            <label>Email</label>
            <input name="email" type="email" value={form.email}
              onChange={handle} placeholder="you@example.com" autoFocus />
          </div>
          <div className="field">
            <label>Password</label>
            <input name="password" type="password" value={form.password}
              onChange={handle} placeholder="••••••••" />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center', marginTop: 4 }}>
            {loading ? 'Signing in…' : 'Sign In →'}
          </button>
        </form>
        <p className="auth-switch">
          Don't have an account?{' '}
          <button onClick={onSwitch}>Create one</button>
        </p>
        <div style={{ marginTop: 20, padding: '12px 14px', background: 'var(--bg3)', borderRadius: 8 }}>
          <p style={{ fontSize: 12, color: 'var(--text3)', marginBottom: 6, fontWeight: 700 }}>DEMO CREDENTIALS</p>
          <p style={{ fontSize: 12, color: 'var(--text2)' }}>Admin: <code>admin@demo.com</code> / <code>Password1!</code></p>
          <p style={{ fontSize: 12, color: 'var(--text2)' }}>User: <code>bob@demo.com</code> / <code>Password1!</code></p>
        </div>
      </div>
    </div>
  );
}
