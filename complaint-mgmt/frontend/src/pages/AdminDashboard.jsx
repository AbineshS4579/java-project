// src/pages/AdminDashboard.jsx
import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Badge, Modal, Empty, Spinner } from '../components/ui';
import ComplaintDetail from '../components/ComplaintDetail';

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const [page,        setPage]        = useState('complaints'); // complaints | users | stats
  const [complaints,  setComplaints]  = useState([]);
  const [users,       setUsers]       = useState([]);
  const [stats,       setStats]       = useState({});
  const [departments, setDepartments] = useState([]);
  const [admins,      setAdmins]      = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [selected,    setSelected]    = useState(null);
  const [actionModal, setActionModal] = useState(null); // { type, complaint }
  const [filters,     setFilters]     = useState({ status: '', priority: '' });

  const loadComplaints = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.allComplaints(filters);
      setComplaints(data);
    } finally { setLoading(false); }
  }, [filters]);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try { setUsers(await api.adminUsers()); } finally { setLoading(false); }
  }, []);

  const loadStats = useCallback(async () => {
    setLoading(true);
    try {
      const [s, d] = await Promise.all([api.stats(), api.departments()]);
      setStats(s); setDepartments(d);
      const all = await api.adminUsers();
      setAdmins(all.filter(u => u.role === 'admin'));
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    if (page === 'complaints') loadComplaints();
    else if (page === 'users')  loadUsers();
    else if (page === 'stats')  loadStats();
  }, [page, loadComplaints, loadUsers, loadStats]);

  // Load departments & admins once for action modals
  useEffect(() => {
    api.departments().then(setDepartments).catch(() => {});
    api.adminUsers().then(u => setAdmins(u.filter(x => x.role === 'admin'))).catch(() => {});
  }, []);

  const STATUSES  = ['pending', 'in_progress', 'resolved', 'closed', 'archived'];
  const PRIORITIES = ['low', 'medium', 'high', 'critical'];

  return (
    <div className="layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo"><span>🛡️</span> CMS Admin</div>
        <div className="sidebar-label">Management</div>
        {[
          { key: 'complaints', icon: '📋', label: 'All Complaints' },
          { key: 'users',      icon: '👥', label: 'Users' },
          { key: 'stats',      icon: '📊', label: 'Statistics' },
        ].map(item => (
          <div key={item.key} className={`nav-item${page === item.key ? ' active' : ''}`}
            onClick={() => setPage(item.key)}>
            <span className="icon">{item.icon}</span>{item.label}
          </div>
        ))}
        <div className="sidebar-footer">
          <div style={{ padding: '8px 12px', fontSize: 13, color: 'var(--text2)' }}>
            <div style={{ fontWeight: 600, color: 'var(--text)' }}>{user.name}</div>
            <div style={{ fontSize: 11, background: 'var(--accent-glow)', color: 'var(--accent-h)', display: 'inline-block', padding: '1px 8px', borderRadius: 9999, marginTop: 2 }}>ADMIN</div>
          </div>
          <div className="nav-item" onClick={logout}><span className="icon">🚪</span>Sign Out</div>
        </div>
      </aside>

      <div className="main">
        {/* COMPLAINTS PAGE */}
        {page === 'complaints' && (
          <>
            <div className="topbar">
              <span className="topbar-title">All Complaints</span>
              <span style={{ fontSize: 13, color: 'var(--text3)' }}>{complaints.length} total</span>
            </div>
            <div className="page">
              {/* Filters */}
              <div style={{ display: 'flex', gap: 10, marginBottom: 18, flexWrap: 'wrap' }}>
                <select value={filters.status} onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}
                  style={{ width: 'auto', padding: '8px 12px' }}>
                  <option value="">All Statuses</option>
                  {STATUSES.map(s => <option key={s} value={s} style={{ textTransform: 'capitalize' }}>{s.replace('_', ' ')}</option>)}
                </select>
                <select value={filters.priority} onChange={e => setFilters(f => ({ ...f, priority: e.target.value }))}
                  style={{ width: 'auto', padding: '8px 12px' }}>
                  <option value="">All Priorities</option>
                  {PRIORITIES.map(p => <option key={p} value={p} style={{ textTransform: 'capitalize' }}>{p}</option>)}
                </select>
                <button className="btn btn-ghost btn-sm" onClick={loadComplaints}>🔄 Refresh</button>
              </div>

              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                {loading ? (
                  <div style={{ padding: 40, textAlign: 'center' }}><Spinner size={28} /></div>
                ) : complaints.length === 0 ? (
                  <Empty icon="📭" message="No complaints found." />
                ) : (
                  <table>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Title</th>
                        <th>User</th>
                        <th>Priority</th>
                        <th>Status</th>
                        <th>Assigned</th>
                        <th>Date</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {complaints.map(c => (
                        <tr key={c.id}>
                          <td style={{ color: 'var(--text3)', fontSize: 12 }}>#{c.id}</td>
                          <td style={{ maxWidth: 180 }}>
                            <div style={{ fontWeight: 500, cursor: 'pointer', overflow: 'hidden',
                              textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--accent-h)' }}
                              onClick={() => setSelected(c.id)}>{c.title}</div>
                          </td>
                          <td style={{ fontSize: 13, color: 'var(--text2)' }}>{c.userName}</td>
                          <td><Badge value={c.priority} /></td>
                          <td><Badge value={c.status} /></td>
                          <td style={{ fontSize: 12, color: 'var(--text3)' }}>
                            {c.assignedToName || '—'}
                          </td>
                          <td style={{ fontSize: 12, color: 'var(--text3)' }}>
                            {new Date(c.createdAt).toLocaleDateString()}
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: 4 }}>
                              <button className="btn btn-ghost btn-sm"
                                onClick={() => setActionModal({ type: 'status', complaint: c })}>
                                Status
                              </button>
                              <button className="btn btn-ghost btn-sm"
                                onClick={() => setActionModal({ type: 'assign', complaint: c })}>
                                Assign
                              </button>
                              <button className="btn btn-ghost btn-sm"
                                onClick={() => setActionModal({ type: 'resolve', complaint: c })}>
                                Resolve
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </>
        )}

        {/* USERS PAGE */}
        {page === 'users' && (
          <>
            <div className="topbar">
              <span className="topbar-title">Users</span>
            </div>
            <div className="page">
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                {loading ? <div style={{ padding: 40, textAlign: 'center' }}><Spinner size={28} /></div> :
                  users.length === 0 ? <Empty /> : (
                  <table>
                    <thead>
                      <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Status</th><th>Joined</th></tr>
                    </thead>
                    <tbody>
                      {users.map(u => (
                        <tr key={u.id}>
                          <td style={{ color: 'var(--text3)', fontSize: 12 }}>#{u.id}</td>
                          <td style={{ fontWeight: 500 }}>{u.name}</td>
                          <td style={{ fontSize: 13, color: 'var(--text2)' }}>{u.email}</td>
                          <td><Badge value={u.role} /></td>
                          <td style={{ fontSize: 13, color: 'var(--text2)' }}>{u.department || '—'}</td>
                          <td>
                            <span style={{ fontSize: 12, color: u.active ? 'var(--resolved)' : 'var(--critical)' }}>
                              {u.active ? '● Active' : '● Inactive'}
                            </span>
                          </td>
                          <td style={{ fontSize: 12, color: 'var(--text3)' }}>
                            {new Date(u.createdAt).toLocaleDateString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </>
        )}

        {/* STATS PAGE */}
        {page === 'stats' && (
          <>
            <div className="topbar"><span className="topbar-title">Statistics</span></div>
            <div className="page">
              {loading ? <div style={{ textAlign: 'center', padding: 60 }}><Spinner size={36} /></div> : (
                <>
                  <h3 style={{ marginBottom: 16, color: 'var(--text2)', fontSize: 13, textTransform: 'uppercase', letterSpacing: '.06em' }}>
                    Complaints by Status
                  </h3>
                  <div className="stat-grid" style={{ marginBottom: 32 }}>
                    {Object.entries(stats).map(([k, v]) => (
                      <div key={k} className="stat-card">
                        <div className="stat-val">{v}</div>
                        <div className="stat-lbl">{k.replace('_', ' ')}</div>
                      </div>
                    ))}
                    <div className="stat-card" style={{ border: '1px solid var(--accent)', background: 'var(--accent-glow)' }}>
                      <div className="stat-val">{Object.values(stats).reduce((a, b) => a + b, 0)}</div>
                      <div className="stat-lbl">Total</div>
                    </div>
                  </div>

                  <h3 style={{ marginBottom: 16, color: 'var(--text2)', fontSize: 13, textTransform: 'uppercase', letterSpacing: '.06em' }}>
                    Departments ({departments.length})
                  </h3>
                  <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                    {departments.map(d => (
                      <div key={d.id} className="card" style={{ padding: '12px 18px', display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontSize: 18 }}>🏢</span>
                        <span style={{ fontSize: 14, fontWeight: 500 }}>{d.name}</span>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          </>
        )}
      </div>

      {/* Complaint detail modal */}
      {selected && (
        <ComplaintDetail id={selected} onClose={() => setSelected(null)} onUpdated={loadComplaints} />
      )}

      {/* Action modals */}
      {actionModal?.type === 'status' && (
        <StatusModal complaint={actionModal.complaint}
          onClose={() => setActionModal(null)}
          onDone={() => { setActionModal(null); loadComplaints(); }} />
      )}
      {actionModal?.type === 'assign' && (
        <AssignModal complaint={actionModal.complaint} admins={admins} departments={departments}
          onClose={() => setActionModal(null)}
          onDone={() => { setActionModal(null); loadComplaints(); }} />
      )}
      {actionModal?.type === 'resolve' && (
        <ResolveModal complaint={actionModal.complaint}
          onClose={() => setActionModal(null)}
          onDone={() => { setActionModal(null); loadComplaints(); }} />
      )}
    </div>
  );
}

// ── Action modals ────────────────────────────

function StatusModal({ complaint, onClose, onDone }) {
  const STATUSES = ['pending', 'in_progress', 'resolved', 'closed', 'archived'];
  const [status,  setStatus]  = useState(complaint.status);
  const [note,    setNote]    = useState('');
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const submit = async () => {
    setLoading(true);
    try {
      await api.updateStatus(complaint.id, { status, note });
      onDone();
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  };

  return (
    <Modal title={`Change Status — #${complaint.id}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={loading}>
          {loading ? 'Saving…' : 'Update Status'}
        </button>
      </>}>
      {error && <div className="alert-error" style={{ marginBottom: 16 }}>{error}</div>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="field">
          <label>New Status</label>
          <select value={status} onChange={e => setStatus(e.target.value)}>
            {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Note (optional)</label>
          <textarea value={note} onChange={e => setNote(e.target.value)}
            placeholder="Add a note about this status change…" rows={3} />
        </div>
      </div>
    </Modal>
  );
}

function AssignModal({ complaint, admins, departments, onClose, onDone }) {
  const [agentId, setAgentId] = useState(complaint.assignedTo || '');
  const [deptId,  setDeptId]  = useState(complaint.departmentId || '');
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const submit = async () => {
    setLoading(true);
    try {
      await api.assign(complaint.id, {
        agentId: agentId ? parseInt(agentId) : null,
        deptId:  deptId  ? parseInt(deptId)  : null,
      });
      onDone();
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  };

  return (
    <Modal title={`Assign — #${complaint.id}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={loading}>
          {loading ? 'Assigning…' : 'Assign'}
        </button>
      </>}>
      {error && <div className="alert-error" style={{ marginBottom: 16 }}>{error}</div>}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="field">
          <label>Assign to Agent</label>
          <select value={agentId} onChange={e => setAgentId(e.target.value)}>
            <option value="">— Unassigned —</option>
            {admins.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Department</label>
          <select value={deptId} onChange={e => setDeptId(e.target.value)}>
            <option value="">— No Department —</option>
            {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
      </div>
    </Modal>
  );
}

function ResolveModal({ complaint, onClose, onDone }) {
  const [resolution, setResolution] = useState('');
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState('');

  const submit = async () => {
    if (!resolution.trim()) { setError('Resolution note is required.'); return; }
    setLoading(true);
    try {
      await api.resolve(complaint.id, { resolution });
      onDone();
    } catch (e) { setError(e.message); } finally { setLoading(false); }
  };

  return (
    <Modal title={`Resolve — #${complaint.id}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={loading}>
          {loading ? 'Resolving…' : '✅ Mark Resolved'}
        </button>
      </>}>
      {error && <div className="alert-error" style={{ marginBottom: 16 }}>{error}</div>}
      <div className="field">
        <label>Resolution Notes *</label>
        <textarea value={resolution} onChange={e => setResolution(e.target.value)}
          placeholder="Describe how the issue was resolved…" rows={5} autoFocus />
      </div>
    </Modal>
  );
}
