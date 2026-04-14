const BASE = 'http://localhost:8080/api';

function token() {
  return localStorage.getItem('cms_token');
}

async function request(method, path, body) {
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token() ? { Authorization: `Bearer ${token()}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  };
  const res = await fetch(`${BASE}${path}`, opts);
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Request failed');
  return data.data ?? data.message;
}

export const api = {
  register: (p)    => request('POST', '/auth/register', p),
  login:    (p)    => request('POST', '/auth/login',    p),
  logout:   ()     => request('POST', '/auth/logout'),
  me:       ()     => request('GET',  '/auth/me'),

  myComplaints:    ()      => request('GET',  '/complaints'),
  createComplaint: (p)     => request('POST', '/complaints', p),
  getComplaint:    (id)    => request('GET',  `/complaints/${id}`),
  addComment:      (id, p) => request('POST', `/complaints/${id}/comments`, p),

  allComplaints:   (q={})  => request('GET',
    '/admin/complaints?' + new URLSearchParams(
      Object.fromEntries(Object.entries(q).filter(([,v])=>v))
    ).toString()
  ),
  updateStatus:    (id, p) => request('PUT',    `/complaints/${id}/status`,  p),
  assign:          (id, p) => request('PUT',    `/complaints/${id}/assign`,  p),
  resolve:         (id, p) => request('PUT',    `/complaints/${id}/resolve`, p),
  archive:         (id)    => request('PUT',    `/complaints/${id}/archive`),
  deleteComplaint: (id)    => request('DELETE', `/complaints/${id}`),
  adminUsers:      ()      => request('GET',    '/admin/users'),
  stats:           ()      => request('GET',    '/admin/stats'),
  departments:     ()      => request('GET',    '/departments'),
};
