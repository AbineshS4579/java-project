// src/components/ComplaintDetail.jsx
import { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Modal, Badge, Timeline, CommentList, Spinner } from './ui';
import { useAuth } from '../context/AuthContext';

export default function ComplaintDetail({ id, onClose, onUpdated }) {
  const { user } = useAuth();
  const [complaint, setComplaint] = useState(null);
  const [loading,   setLoading]   = useState(true);
  const [tab,       setTab]       = useState('details'); // details | history | comments
  const [comment,   setComment]   = useState('');
  const [internal,  setInternal]  = useState(false);
  const [posting,   setPosting]   = useState(false);
  const [error,     setError]     = useState('');

  useEffect(() => {
    load();
  }, [id]);

  async function load() {
    setLoading(true);
    try {
      const c = await api.getComplaint(id);
      setComplaint(c);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function submitComment() {
    if (!comment.trim()) return;
    setPosting(true);
    try {
      await api.addComment(id, { body: comment, internal });
      setComment('');
      await load();
      if (onUpdated) onUpdated();
    } catch (e) {
      setError(e.message);
    } finally {
      setPosting(false);
    }
  }

  const TABS = ['details', 'history', 'comments'];

  return (
    <Modal title={loading ? 'Loading…' : `#${complaint?.id} — ${complaint?.title}`} onClose={onClose}>
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}><Spinner size={32} /></div>
      ) : error ? (
        <div className="alert-error">{error}</div>
      ) : (
        <>
          {/* Meta row */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
            <Badge value={complaint.status} />
            <Badge value={complaint.priority} />
            <span style={{ fontSize: 12, color: 'var(--text3)', marginLeft: 'auto' }}>
              {new Date(complaint.createdAt).toLocaleDateString()}
            </span>
          </div>

          {/* Tabs */}
          <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--border)', marginBottom: 20 }}>
            {TABS.map(t => (
              <button key={t} onClick={() => setTab(t)} style={{
                background: 'none', border: 'none', cursor: 'pointer',
                padding: '8px 14px', fontSize: 13, fontWeight: 600,
                color: tab === t ? 'var(--accent-h)' : 'var(--text3)',
                borderBottom: `2px solid ${tab === t ? 'var(--accent)' : 'transparent'}`,
                textTransform: 'capitalize'
              }}>{t}</button>
            ))}
          </div>

          {/* Tab content */}
          {tab === 'details' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <InfoRow label="Category"    value={complaint.category} />
              <InfoRow label="Submitted by" value={complaint.userName} />
              {complaint.assignedToName && <InfoRow label="Assigned to" value={complaint.assignedToName} />}
              {complaint.departmentName  && <InfoRow label="Department"  value={complaint.departmentName} />}
              <div className="field">
                <label>Description</label>
                <div style={{ fontSize: 14, color: 'var(--text)', lineHeight: 1.6,
                  background: 'var(--bg3)', padding: '12px 14px', borderRadius: 8,
                  whiteSpace: 'pre-wrap' }}>
                  {complaint.description}
                </div>
              </div>
              {complaint.resolution && (
                <div className="field">
                  <label style={{ color: 'var(--resolved)' }}>✅ Resolution</label>
                  <div style={{ fontSize: 14, color: 'var(--text)', lineHeight: 1.6,
                    background: 'rgba(34,197,94,.08)', border: '1px solid rgba(34,197,94,.2)',
                    padding: '12px 14px', borderRadius: 8 }}>
                    {complaint.resolution}
                  </div>
                </div>
              )}
            </div>
          )}

          {tab === 'history' && <Timeline items={complaint.history} />}

          {tab === 'comments' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <CommentList comments={complaint.comments} isAdmin={user?.role === 'admin'} />
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: 16 }}>
                <div className="field">
                  <label>Add Comment</label>
                  <textarea value={comment} onChange={e => setComment(e.target.value)}
                    placeholder="Write a comment…" rows={3} />
                </div>
                {user?.role === 'admin' && (
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8,
                    fontSize: 13, color: 'var(--text2)', marginTop: 8, cursor: 'pointer' }}>
                    <input type="checkbox" checked={internal}
                      onChange={e => setInternal(e.target.checked)} />
                    Internal note (admins only)
                  </label>
                )}
                <button className="btn btn-primary btn-sm" onClick={submitComment}
                  disabled={posting || !comment.trim()} style={{ marginTop: 10 }}>
                  {posting ? 'Posting…' : 'Post Comment'}
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </Modal>
  );
}

function InfoRow({ label, value }) {
  return (
    <div style={{ display: 'flex', gap: 12 }}>
      <span style={{ fontSize: 13, color: 'var(--text3)', width: 110, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 14, color: 'var(--text)' }}>{value}</span>
    </div>
  );
}
