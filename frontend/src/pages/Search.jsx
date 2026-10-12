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

      <form className="card search-bar" onSubmit={search}>
        <input name="from" placeholder="From (e.g. Tiruppur)" value={filters.from} onChange={change} />
        <input name="to" placeholder="To (e.g. Chennai)" value={filters.to} onChange={change} />
        <input name="date" type="date" value={filters.date} onChange={change} />
        <select name="sort" value={filters.sort} onChange={change}>
          <option value="date">Sort: Date</option>
          <option value="price">Sort: Price (low to high)</option>
        </select>
        <label className="check"><input type="checkbox" name="ladiesOnly" checked={filters.ladiesOnly} onChange={change} /> Ladies only</label>
        <button className="btn" type="submit">Search</button>
      </form>

      {message.text && <p className={message.type}>{message.text}</p>}
      {rides.length === 0 && <p className="muted center">No rides found.</p>}
      {rides.map((r) => (
        <RideCard key={r.id} ride={r} action={<RequestAction ride={r} onRequest={request} />} />
      ))}
    </>
  );
}
