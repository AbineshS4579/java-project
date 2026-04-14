// src/pages/RegisterPage.jsx
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage({ onSwitch }) {
  const { register } = useAuth();
  const [form,    setForm]    = useState({ name: '', email: '', password: '', confirm: '' });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const handle = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async e => {
    e.preventDefault();
    setError('');
    if (!form.name || !form.email || !form.password) { setError('All fields required.'); return; }
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return; }
    if (form.password !== form.confirm) { setError('Passwords do not match.'); return; }
    setLoading(true);
    try {
      await register({ name: form.name, email: form.email, password: form.password, role: 'user' });
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
          <div style={{ fontSize: 40, marginBottom: 8 }}>📝</div>
          <h1>Create account</h1>
          <p>Get started with Complaint Management</p>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {error && <div className="alert-error">{error}</div>}
          <div className="field">
            <label>Full Name</label>
            <input name="name" value={form.name} onChange={handle} placeholder="Jane Smith" autoFocus />
          </div>
          <div className="field">
            <label>Email</label>
            <input name="email" type="email" value={form.email} onChange={handle} placeholder="you@example.com" />
          </div>
          <div className="field">
            <label>Password</label>
            <input name="password" type="password" value={form.password} onChange={handle} placeholder="Min 8 characters" />
          </div>
          <div className="field">
            <label>Confirm Password</label>
            <input name="confirm" type="password" value={form.confirm} onChange={handle} placeholder="Re-enter password" />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading}
            style={{ width: '100%', justifyContent: 'center', marginTop: 4 }}>
            {loading ? 'Creating…' : 'Create Account →'}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account?{' '}
          <button onClick={onSwitch}>Sign in</button>
        </p>
      </div>
    </div>
  );
}
