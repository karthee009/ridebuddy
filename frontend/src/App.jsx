import { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { api } from './api';
import { useAuth } from './AuthContext';
import Search from './pages/Search';
import Auth from './pages/Auth';
import PostRide from './pages/PostRide';
import MyTrips from './pages/MyTrips';
import Vehicles from './pages/Vehicles';
import Profile from './pages/Profile';
import Notifications from './pages/Notifications';

function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);

  // Poll for new notifications every 15 seconds (and when another page asks for a refresh).
  useEffect(() => {
    if (!user) { setUnread(0); return; }
    const load = () =>
      api.get('/notifications').then((l) => setUnread(l.filter((n) => !n.seen).length)).catch(() => {});
    load();
    const timer = setInterval(load, 15000);
    window.addEventListener('notif-refresh', load);
    return () => { clearInterval(timer); window.removeEventListener('notif-refresh', load); };
  }, [user]);

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <Link to="/" className="brand">RideBuddy</Link>
      <div className="nav-links">
        <Link to="/">Find Ride</Link>
        {user ? (
          <>
            <Link to="/post">Offer Ride</Link>
            <Link to="/my-trips">My Trips</Link>
            <Link to="/vehicles">Vehicles</Link>
            <Link to="/notifications">Alerts{unread > 0 && <span className="count">{unread}</span>}</Link>
            <Link to="/profile">{user.name}</Link>
            <button className="btn btn-light" onClick={handleLogout}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </div>
    </nav>
  );
}

// Pages that need a login are wrapped in this component.
function Protected({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <p className="center muted">Loading...</p>;
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Search />} />
          <Route path="/login" element={<Auth mode="login" />} />
          <Route path="/register" element={<Auth mode="register" />} />
          <Route path="/post" element={<Protected><PostRide /></Protected>} />
          <Route path="/my-trips" element={<Protected><MyTrips /></Protected>} />
          <Route path="/vehicles" element={<Protected><Vehicles /></Protected>} />
          <Route path="/notifications" element={<Protected><Notifications /></Protected>} />
          <Route path="/profile" element={<Protected><Profile /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
