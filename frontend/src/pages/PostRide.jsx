import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';

export default function PostRide() {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState({
    source: '', destination: '', rideDate: '', price: '', seats: '', vehicleId: '',
    ladiesOnly: false, noSmoking: false, petsAllowed: false,
  });
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/vehicles').then((list) => {
      setVehicles(list);
      if (list.length > 0) setForm((f) => ({ ...f, vehicleId: String(list[0].id) }));
    }).catch(() => {});
  }, []);

  const change = (e) => {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/rides', {
        ...form, price: Number(form.price), seats: Number(form.seats), vehicleId: Number(form.vehicleId),
      });
      navigate('/my-trips');
    } catch (err) { setError(err.message); }
  };

  if (vehicles.length === 0) {
    return (
      <div className="card form-card">
        <h2>Offer a ride</h2>
        <p>You need to add a vehicle before offering a ride.</p>
        <Link className="btn" to="/vehicles">Add vehicle</Link>
      </div>
    );
  }

  return (
    <div className="card form-card">
      <h2>Offer a ride</h2>
      {error && <p className="error">{error}</p>}
      <form onSubmit={submit}>
        <input name="source" placeholder="From" value={form.source} onChange={change} required />
        <input name="destination" placeholder="To" value={form.destination} onChange={change} required />
        <input name="rideDate" type="date" value={form.rideDate} onChange={change} required />
        <select name="vehicleId" value={form.vehicleId} onChange={change}>
          {vehicles.map((v) => (
            <option key={v.id} value={v.id}>{v.color} {v.brand} {v.model} ({v.vehicleNumber})</option>
          ))}
        </select>
        <input name="price" type="number" min="1" placeholder="Price per seat (Rs)" value={form.price} onChange={change} required />
        <input name="seats" type="number" min="1" max="7" placeholder="Seats to offer" value={form.seats} onChange={change} required />
        <label className="check"><input type="checkbox" name="ladiesOnly" checked={form.ladiesOnly} onChange={change} /> Ladies only</label>
        <label className="check"><input type="checkbox" name="noSmoking" checked={form.noSmoking} onChange={change} /> No smoking</label>
        <label className="check"><input type="checkbox" name="petsAllowed" checked={form.petsAllowed} onChange={change} /> Pets allowed</label>
        <button className="btn" type="submit">Post ride</button>
      </form>
    </div>
  );
}
