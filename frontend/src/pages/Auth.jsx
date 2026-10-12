import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';

// One component for both Login and Register. `mode` decides which one.
export default function Auth({ mode }) {
  const isRegister = mode === 'register';
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isRegister) await register(form.name, form.email, form.password);
      else await login(form.email, form.password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="card form-card">
      <h2>{isRegister ? 'Create account' : 'Welcome back'}</h2>
      {error && <p className="error">{error}</p>}
      <form onSubmit={submit}>
        {isRegister && (
          <input name="name" placeholder="Full name" value={form.name} onChange={change} required />
        )}
        <input name="email" type="email" placeholder="Email" value={form.email} onChange={change} required />
        <input name="password" type="password" placeholder="Password (min 6 characters)"
               value={form.password} onChange={change} required />
        <button className="btn" type="submit">{isRegister ? 'Register' : 'Login'}</button>
      </form>
      <p className="muted center">
        {isRegister
          ? <>Already have an account? <Link to="/login">Login</Link></>
          : <>New here? <Link to="/register">Register</Link></>}
      </p>
    </div>
  );
}
