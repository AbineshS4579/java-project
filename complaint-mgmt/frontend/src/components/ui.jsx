// src/components/ui.jsx
export function Badge({ value, type = 'status' }) {
  if (!value) return null;
  const key = value.toLowerCase().replace(' ', '_');
  return <span className={`badge badge-${key}`}>{value.replace('_', ' ')}</span>;
}

export function Modal({ title, onClose, children, footer }) {
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        {children}
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

export function Spinner({ size = 24 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      border: '2px solid var(--border)', borderTopColor: 'var(--accent)',
      animation: 'spin .7s linear infinite', display: 'inline-block'
    }} />
  );
}

export function Empty({ icon = '📭', message = 'Nothing here yet.' }) {
  return (
    <div className="empty">
      <div className="icon">{icon}</div>
      <p>{message}</p>
    </div>
  );
}

export function Timeline({ items }) {
  if (!items?.length) return <Empty icon="📋" message="No status history yet." />;
  return (
    <div className="timeline">
      {items.map((item, i) => (
        <div className="tl-item" key={i}>
          <div className="tl-line">
            <div className="tl-dot" />
            <div className="tl-connector" />
          </div>
          <div className="tl-body">
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
              {item.oldStatus && <Badge value={item.oldStatus} />}
              {item.oldStatus && <span style={{ color: 'var(--text3)' }}>→</span>}
              <Badge value={item.newStatus} />
            </div>
            {item.note && (
              <p style={{ fontSize: 13, color: 'var(--text2)', marginTop: 4 }}>{item.note}</p>
            )}
            <p className="tl-meta">
              by <strong style={{ color: 'var(--text2)' }}>{item.changedByName}</strong>{' '}
              &bull; {new Date(item.changedAt).toLocaleString()}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}

export function CommentList({ comments = [], isAdmin }) {
  const visible = isAdmin ? comments : comments.filter(c => !c.internal);
  if (!visible.length) return <Empty icon="💬" message="No comments yet." />;
  return (
    <div className="comment-list">
      {visible.map(c => (
        <div key={c.id} className={`comment-item${c.internal ? ' internal' : ''}`}>
          <p className="comment-meta">
            <strong>{c.userName}</strong>
            {c.internal && <span style={{ color: 'var(--accent)', marginLeft: 6, fontSize: 10, fontWeight: 700 }}>INTERNAL</span>}
            {' · '}{new Date(c.createdAt).toLocaleString()}
          </p>
          <p style={{ fontSize: 14, whiteSpace: 'pre-wrap' }}>{c.body}</p>
        </div>
      ))}
    </div>
  );
}
