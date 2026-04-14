// src/pages/UserDashboard.jsx
import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Badge, Modal, Empty, Spinner } from '../components/ui';
import ComplaintDetail from '../components/ComplaintDetail';

const CATEGORIES = ['Technical', 'Billing', 'Performance', 'Feature', 'Account', 'Other'];
const PRIORITIES  = ['low', 'medium', 'high', 'critical'];

export default function UserDashboard() {
  const { user, logout } = useAuth();
  const [complaints, setComplaints] = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [view,       setView]       = useState('list'); // list | new
  const [selected,   setSelected]   = useState(null);
  const [filter,     setFilter]     = useState('all');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.myComplaints();
      setComplaints(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = filter === 'all'
    ? complaints
    : complaints.filter(c => c.status === filter);

  const counts = {
    all:         complaints.length,
    pending:     complaints.filter(c => c.status === 'pending').length,
    in_progress: complaints.filter(c => c.status === 'in_progress').length,
    resolved:    complaints.filter(c => c.status === 'resolved').length,
  };

  return (
    <div className="layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span>🗂️</span> CMS
        </div>
        <div className="sidebar-label">Navigation</div>
        {[
          { key: 'list', icon: '📋', label: 'My Complaints' },
          { key: 'new',  icon: '➕', label: 'New Complaint' },
        ].map(item => (
          <div key={item.key}
            className={`nav-item${view === item.key ? ' active' : ''}`}
            onClick={() => setView(item.key)}>
            <span className="icon">{item.icon}</span>
            {item.label}
          </div>
        ))}
        <div className="sidebar-footer">
          <div style={{ padding: '8px 12px', fontSize: 13, color: 'var(--text2)' }}>
            <div style={{ fontWeight: 600, color: 'var(--text)' }}>{user.name}</div>
            <div style={{ fontSize: 11, color: 'var(--text3)' }}>{user.email}</div>
          </div>
          <div className="nav-item" onClick={logout}>
            <span className="icon">🚪</span> Sign Out
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="main">
        {view === 'list' ? (
          <>
            <div className="topbar">
              <span className="topbar-title">My Complaints</span>
              <button className="btn btn-primary btn-sm" onClick={() => setView('new')}>
                ➕ New Complaint
              </button>
            </div>
            <div className="page">
              {/* Stats */}
              <div className="stat-grid" style={{ marginBottom: 24 }}>
                {Object.entries(counts).map(([k, v]) => (
                  <div key={k} className="stat-card" style={{ cursor: 'pointer' }}
                    onClick={() => setFilter(k)}>
                    <div className="stat-val">{v}</div>
                    <div className="stat-lbl">{k.replace('_', ' ')}</div>
                  </div>
                ))}
              </div>

              {/* Filter tabs */}
              <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
                {Object.keys(counts).map(k => (
                  <button key={k} onClick={() => setFilter(k)}
                    className={`btn btn-sm ${filter === k ? 'btn-primary' : 'btn-ghost'}`}
                    style={{ textTransform: 'capitalize' }}>
                    {k.replace('_', ' ')} ({counts[k]})
                  </button>
                ))}
              </div>

              {/* Table */}
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                {loading ? (
                  <div style={{ padding: 40, textAlign: 'center' }}><Spinner size={28} /></div>
                ) : filtered.length === 0 ? (
                  <Empty icon="📭" message="No complaints in this category." />
                ) : (
                  <table>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Title</th>
                        <th>Category</th>
                        <th>Priority</th>
                        <th>Status</th>
                        <th>Submitted</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filtered.map(c => (
                        <tr key={c.id} style={{ cursor: 'pointer' }}
                          onClick={() => setSelected(c.id)}>
                          <td style={{ color: 'var(--text3)', fontFamily: 'var(--mono)', fontSize: 12 }}>#{c.id}</td>
                          <td style={{ maxWidth: 240 }}>
                            <div style={{ fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {c.title}
                            </div>
                          </td>
                          <td style={{ color: 'var(--text2)', fontSize: 13 }}>{c.category}</td>
                          <td><Badge value={c.priority} /></td>
                          <td><Badge value={c.status} /></td>
                          <td style={{ fontSize: 12, color: 'var(--text3)' }}>
                            {new Date(c.createdAt).toLocaleDateString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </>
        ) : (
          <NewComplaintForm onSuccess={() => { setView('list'); load(); }} onCancel={() => setView('list')} />
        )}
      </div>

      {/* Detail modal */}
      {selected && (
        <ComplaintDetail id={selected} onClose={() => setSelected(null)} onUpdated={load} />
      )}
    </div>
  );
}

function NewComplaintForm({ onSuccess, onCancel }) {
  const [form,    setForm]    = useState({ title: '', description: '', category: 'Technical', priority: 'medium' });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handle = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async e => {
    e.preventDefault();
    setError('');
    if (!form.title.trim() || !form.description.trim()) {
      setError('Title and description are required.'); return;
    }
    setLoading(true);
    try {
      await api.createComplaint(form);
      setSuccess(true);
      setTimeout(onSuccess, 1200);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="topbar">
        <span className="topbar-title">Submit New Complaint</span>
        <button className="btn btn-ghost btn-sm" onClick={onCancel}>← Back</button>
      </div>
      <div className="page">
        <div className="card" style={{ maxWidth: 640 }}>
          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {error   && <div className="alert-error">{error}</div>}
            {success && <div className="alert-success">✅ Complaint submitted successfully!</div>}

            <div className="field">
              <label>Title *</label>
              <input name="title" value={form.title} onChange={handle}
                placeholder="Brief summary of the issue" />
            </div>
            <div className="field">
              <label>Description *</label>
              <textarea name="description" value={form.description} onChange={handle}
                placeholder="Describe the issue in detail — what happened, when, steps to reproduce…"
                rows={5} />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="field">
                <label>Category</label>
                <select name="category" value={form.category} onChange={handle}>
                  {CATEGORIES.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Priority</label>
                <select name="priority" value={form.priority} onChange={handle}>
                  {PRIORITIES.map(p => <option key={p} value={p} style={{ textTransform: 'capitalize' }}>{p}</option>)}
                </select>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={loading || success}>
                {loading ? 'Submitting…' : '📤 Submit Complaint'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
}
