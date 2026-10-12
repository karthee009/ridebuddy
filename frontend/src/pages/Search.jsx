import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../AuthContext';
import RideCard from '../components/RideCard';

// Seat selector + request button shown on every ride card.
function RequestAction({ ride, onRequest }) {
  const [n, setN] = useState(1);
  if (ride.seats === 0) return <button className="btn" disabled>Full</button>;
  return (
    <div className="req-action">
      <select value={n} onChange={(e) => setN(Number(e.target.value))}>
        {Array.from({ length: ride.seats }, (_, i) => (
          <option key={i + 1} value={i + 1}>{i + 1} seat(s)</option>
        ))}
      </select>
      <button className="btn" onClick={() => onRequest(ride.id, n)}>Request</button>
    </div>
  );
}

export default function Search() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ from: '', to: '', date: '', ladiesOnly: false, sort: 'date' });
  const [rides, setRides] = useState([]);
  const [message, setMessage] = useState({ type: '', text: '' });

  const load = async () => {
    const q = new URLSearchParams({
      from: filters.from, to: filters.to, sort: filters.sort, ladiesOnly: String(filters.ladiesOnly),
    });
    if (filters.date) q.set('date', filters.date);
    try {
      setRides(await api.get('/rides?' + q.toString()));
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  };

  useEffect(() => { load(); }, []);

  const change = (e) => {
    const { name, type, checked, value } = e.target;
    setFilters({ ...filters, [name]: type === 'checkbox' ? checked : value });
  };

  const search = (e) => { e.preventDefault(); load(); };

  const request = async (id, seats) => {
    if (!user) return navigate('/login');
    try {
      await api.post(`/rides/${id}/book`, { seats });
      setMessage({ type: 'success', text: 'Request sent! Wait for the driver to accept. See My Trips.' });
      load();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  };

  return (
    <>
      <section className="home-hero">
        <div className="hero-copy">
          <span className="hero-eyebrow">Better journeys start together</span>
          <h1>Your next ride.<br />Your kind of journey.</h1>
          <p>Find a seat, share the journey, and make every mile a little easier with RideBuddy.</p>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-road"><div className="hero-car">↗</div></div>
        </div>
      </section>

      <div className="search-section-title">
        <div>
          <h2>Where are you headed?</h2>
          <p className="muted">Search available rides and choose what works for you.</p>
        </div>
      </div>
      <form className="card search-bar" onSubmit={search}>
        <input name="from" aria-label="From" placeholder="From · e.g. Coimbatore" value={filters.from} onChange={change} />
        <input name="to" aria-label="To" placeholder="To · e.g. Chennai" value={filters.to} onChange={change} />
        <input name="date" aria-label="Travel date" type="date" value={filters.date} onChange={change} />
        <select name="sort" aria-label="Sort rides" value={filters.sort} onChange={change}>
          <option value="date">Sort by: Travel date</option>
          <option value="price">Sort by: Lowest price</option>
        </select>
        <label className="check"><input type="checkbox" name="ladiesOnly" checked={filters.ladiesOnly} onChange={change} /> Ladies-only rides</label>
        <button className="btn" type="submit">Search rides <span aria-hidden="true">→</span></button>
      </form>

      {message.text && <p className={message.type}>{message.text}</p>}
      <section className="ride-results">
        <div className="search-section-title">
          <div>
            <h2>Available rides</h2>
            <p className="muted">{rides.length} {rides.length === 1 ? 'ride' : 'rides'} to explore</p>
          </div>
        </div>
        {rides.length === 0 && (
          <div className="empty-state">
            <div className="empty-icon" aria-hidden="true">⌕</div>
            <h3>No rides found just yet</h3>
            <p className="muted">Try another route or date, or clear a filter to see more options.</p>
          </div>
        )}
        {rides.map((r) => (
          <RideCard key={r.id} ride={r} action={<RequestAction ride={r} onRequest={request} />} />
        ))}
      </section>
    </>
  );
}
