import { useEffect, useState } from 'react';
import { api } from '../api';

export default function Notifications() {
  const [list, setList] = useState([]);

  useEffect(() => {
    api.get('/notifications').then(setList).catch(() => {});
    // Mark everything as read, then tell the navbar to refresh its counter.
    api.post('/notifications/read')
      .then(() => window.dispatchEvent(new Event('notif-refresh')))
      .catch(() => {});
  }, []);

  return (
    <>
      <h2>Alerts</h2>
      {list.length === 0 && <p className="muted">No notifications.</p>}
      {list.map((n) => (
        <div className={`card ${n.seen ? '' : 'unread'}`} key={n.id}>
          <div>{n.message}</div>
          <div className="muted">{new Date(n.createdAt).toLocaleString()}</div>
        </div>
      ))}
    </>
  );
}
