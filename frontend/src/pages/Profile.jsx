import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../AuthContext';

export default function Profile() {
  const { setUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: '', phone: '' });
  const [reviews, setReviews] = useState([]);
  const [msg, setMsg] = useState({ type: '', text: '' });

  useEffect(() => {
    api.get('/profile').then((p) => {
      setProfile(p);
      setForm({ name: p.name, phone: p.phone || '' });
      api.get(`/users/${p.id}/reviews`).then(setReviews).catch(() => {});
    });
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setMsg({ type: '', text: '' });
    try {
      const p = await api.put('/profile', form);
      setProfile(p);
      setUser(p);
      setMsg({ type: 'success', text: 'Profile updated' });
    } catch (err) { setMsg({ type: 'error', text: err.message }); }
  };

  if (!profile) return <p className="muted center">Loading...</p>;

  return (
    <>
      <div className="card form-card wide">
        <h2>My profile</h2>
        <p className="muted">{profile.email} &middot; Rating: {profile.ratingCount > 0 ? `${profile.avgRating} (${profile.ratingCount} reviews)` : 'no ratings yet'}</p>
        {msg.text && <p className={msg.type}>{msg.text}</p>}
        <form onSubmit={save}>
          <input placeholder="Full name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <input placeholder="Phone (10 digits)" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <button className="btn" type="submit">Save</button>
        </form>
      </div>

      <h2>Reviews about me</h2>
      {reviews.length === 0 && <p className="muted">No reviews yet.</p>}
      {reviews.map((r) => (
        <div className="card" key={r.id}>
          <b>{r.rating} star(s)</b> from {r.reviewerName}
          {r.comment && <div className="muted">{r.comment}</div>}
        </div>
      ))}
    </>
  );
}
