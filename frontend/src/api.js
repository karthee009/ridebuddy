// One helper for every backend call.
// credentials: 'include' makes the browser send the JSESSIONID session cookie.
async function request(path, options = {}) {
  const res = await fetch('/api' + path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.message || 'Something went wrong');
  return data;
}

const body = (b) => (b ? JSON.stringify(b) : undefined);

export const api = {
  get: (path) => request(path),
  post: (path, b) => request(path, { method: 'POST', body: body(b) }),
  put: (path, b) => request(path, { method: 'PUT', body: body(b) }),
  del: (path) => request(path, { method: 'DELETE' }),
};
