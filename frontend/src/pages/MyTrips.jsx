import { useEffect, useState } from 'react';
import { api } from '../api';
import RideCard from '../components/RideCard';

function Badge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{status}</span>;
}

// Runs an API call, then refreshes the page data. Shows the error message if it fails.
async function run(call, setError, onChanged) {
  setError('');
  try { await call(); onChanged(); } catch (e) { setError(e.message); }
}

// Star rating (1-5) + optional comment, for a completed trip.
function RateForm({ bookingId, label, onDone }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');
  const submit = () => run(() => api.post('/reviews', { bookingId, rating, comment }), setError, onDone);
  return (
    <div className="decline-box">
      <b>{label}</b>
      <div className="actions">
        <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
          {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} star(s)</option>)}
        </select>
        <input placeholder="Comment (optional)" maxLength="255" value={comment} onChange={(e) => setComment(e.target.value)} />
        <button className="btn" onClick={submit}>Submit</button>
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

// DRIVER side: a request received for my ride -> Accept / Decline (reason required).
function RequestCard({ booking, onChanged }) {
  const [declining, setDeclining] = useState(false);
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const { ride } = booking;

  const accept = () => run(() => api.post(`/bookings/${booking.id}/accept`), setError, onChanged);
  const decline = () => {
    if (!reason.trim()) return setError('Please enter a reason for declining');
    run(() => api.post(`/bookings/${booking.id}/decline`, { reason }), setError, onChanged);
  };

  return (
    <div className="card">
      <div className="req-head">
        <div>
          <b>{booking.passengerName}</b> requested {booking.seats} seat(s) on <b>{ride.source} &rarr; {ride.destination}</b>
          <div className="muted">{ride.rideDate} &middot; {ride.seats} seat(s) left</div>
        </div>
        <Badge status={booking.status} />
      </div>

      {booking.status === 'DECLINED' && <p className="muted">Reason: {booking.declineReason}</p>}
      {booking.status === 'ACCEPTED' && (
        <p className="muted">Passenger phone: {booking.passengerPhone || 'not provided'}</p>
      )}

      {booking.status === 'PENDING' && !declining && (
        <div className="actions">
          <button className="btn" onClick={accept}>Accept</button>
          <button className="btn btn-danger" onClick={() => setDeclining(true)}>Decline</button>
        </div>
      )}
      {booking.status === 'PENDING' && declining && (
        <div className="decline-box">
          <textarea rows="2" maxLength="255" placeholder="Reason for declining (required)"
                    value={reason} onChange={(e) => setReason(e.target.value)} />
          <div className="actions">
            <button className="btn btn-danger" onClick={decline}>Confirm decline</button>
            <button className="btn btn-light-border" onClick={() => setDeclining(false)}>Cancel</button>
          </div>
        </div>
      )}

      {booking.status === 'ACCEPTED' && ride.status === 'COMPLETED' && !booking.reviewedByMe && (
        <RateForm bookingId={booking.id} label="Rate this passenger" onDone={onChanged} />
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}

// DRIVER side: a ride I offered -> Start / Complete / Cancel.
function OfferedRide({ ride, onChanged }) {
  const [error, setError] = useState('');
  const act = (action) => run(() => api.post(`/rides/${ride.id}/${action}`), setError, onChanged);
  const cancel = () => { if (window.confirm('Cancel this ride? All requests will be cancelled.')) act('cancel'); };

  return (
    <RideCard ride={ride}>
      <div className="actions">
        {ride.status === 'SCHEDULED' && <button className="btn" onClick={() => act('start')}>Start trip</button>}
        {ride.status === 'STARTED' && <button className="btn" onClick={() => act('complete')}>Complete trip</button>}
        {ride.status === 'SCHEDULED' && <button className="btn btn-danger" onClick={cancel}>Cancel ride</button>}
      </div>
      {error && <p className="error">{error}</p>}
    </RideCard>
  );
}

// PASSENGER side: my own request -> status, contact details, cancel, rate.
function MyBooking({ booking, onChanged }) {
  const [error, setError] = useState('');
  const { ride } = booking;
  const cancel = () => {
    if (window.confirm('Cancel this request?'))
      run(() => api.post(`/bookings/${booking.id}/cancel`), setError, onChanged);
  };
  const canCancel = (booking.status === 'PENDING' || booking.status === 'ACCEPTED') && ride.status === 'SCHEDULED';

  return (
    <RideCard ride={ride} action={<Badge status={booking.status} />}>
      <p className="muted">Seats requested: {booking.seats}</p>
      {booking.status === 'DECLINED' && <p className="error">Declined by driver: {booking.declineReason}</p>}
      {booking.status === 'ACCEPTED' && (
        <p className="success">
          Confirmed. Driver phone: {booking.driverPhone || 'not provided'} &middot; Vehicle number: {booking.vehicleNumber}
        </p>
      )}
      {canCancel && <div className="actions"><button className="btn btn-danger" onClick={cancel}>Cancel request</button></div>}
      {booking.status === 'ACCEPTED' && ride.status === 'COMPLETED' && !booking.reviewedByMe && (
        <RateForm bookingId={booking.id} label="Rate your driver" onDone={onChanged} />
      )}
      {error && <p className="error">{error}</p>}
    </RideCard>
  );
}

export default function MyTrips() {
  const [posted, setPosted] = useState([]);
  const [requests, setRequests] = useState([]);
  const [booked, setBooked] = useState([]);

  const load = () => {
    api.get('/rides/mine').then(setPosted).catch(() => {});
    api.get('/bookings/requests').then(setRequests).catch(() => {});
    api.get('/bookings/mine').then(setBooked).catch(() => {});
  };
  useEffect(load, []);

  return (
    <>
      <h2>Requests for my rides</h2>
      {requests.length === 0 && <p className="muted">No requests yet.</p>}
      {requests.map((b) => <RequestCard key={b.id} booking={b} onChanged={load} />)}

      <h2>Rides I offered</h2>
      {posted.length === 0 && <p className="muted">You have not offered any rides.</p>}
      {posted.map((r) => <OfferedRide key={r.id} ride={r} onChanged={load} />)}

      <h2>My booking requests</h2>
      {booked.length === 0 && <p className="muted">You have not requested any rides.</p>}
      {booked.map((b) => <MyBooking key={b.id} booking={b} onChanged={load} />)}
    </>
  );
}
