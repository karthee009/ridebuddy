import { useEffect, useState } from 'react';
import { api } from '../api';

const empty = { vehicleType: 'CAR', brand: '', model: '', color: '', vehicleNumber: '', totalSeats: 5, ac: true, fuelType: 'PETROL' };

export default function Vehicles() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState(empty);
  const [error, setError] = useState('');

  const load = () => api.get('/vehicles').then(setList).catch(() => {});
  useEffect(() => { load(); }, []);

  const change = (e) => {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const add = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/vehicles', { ...form, totalSeats: Number(form.totalSeats) });
      setForm(empty);
      load();
    } catch (err) { setError(err.message); }
  };

  const remove = async (id) => {
    if (!window.confirm('Delete this vehicle?')) return;
    setError('');
    try { await api.del('/vehicles/' + id); load(); } catch (err) { setError(err.message); }
  };

  return (
    <>
      <h2>My vehicles</h2>
      {error && <p className="error">{error}</p>}
      {list.length === 0 && <p className="muted">Add a vehicle first, then you can offer rides.</p>}
      {list.map((v) => (
        <div className="card ride" key={v.id}>
          <div>
            <div className="route">{v.color} {v.brand} {v.model}</div>
            <div className="muted">{v.vehicleNumber} &middot; {v.vehicleType} &middot; {v.totalSeats} seats &middot; {v.ac ? 'AC' : 'Non-AC'} &middot; {v.fuelType}</div>
          </div>
          <button className="btn btn-danger" onClick={() => remove(v.id)}>Delete</button>
        </div>
      ))}

      <div className="card form-card wide">
        <h3>Add a vehicle</h3>
        <form onSubmit={add}>
          <select name="vehicleType" value={form.vehicleType} onChange={change}>
            <option>CAR</option><option>SUV</option><option>BIKE</option>
          </select>
          <input name="brand" placeholder="Brand (e.g. Maruti)" value={form.brand} onChange={change} required />
          <input name="model" placeholder="Model (e.g. Swift)" value={form.model} onChange={change} required />
          <input name="color" placeholder="Colour" value={form.color} onChange={change} required />
          <input name="vehicleNumber" placeholder="Vehicle number (TN39AB1234)" maxLength="15" value={form.vehicleNumber} onChange={change} required />
          <input name="totalSeats" type="number" min="1" max="8" placeholder="Total seats (with driver)" value={form.totalSeats} onChange={change} required />
          <select name="fuelType" value={form.fuelType} onChange={change}>
            <option>PETROL</option><option>DIESEL</option><option>CNG</option><option>ELECTRIC</option>
          </select>
          <label className="check"><input type="checkbox" name="ac" checked={form.ac} onChange={change} /> Air conditioner (AC)</label>
          <button className="btn" type="submit">Add vehicle</button>
        </form>
      </div>
    </>
  );
}
