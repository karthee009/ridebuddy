
export default function RideCard({ ride, action, children }) {

  const vehicleType = String(ride.vehicle || '').toLowerCase();

  // Detect bike / motorcycle
  const isBike =
    vehicleType.includes('bike') ||
    vehicleType.includes('motorcycle');

  return (
    <div className="card">

      {/* Cinematic vehicle animation */}
      <div className="vehicle-scene">

        <div className="road-lines"></div>

        <div className="speed-lines"></div>

        {isBike ? (
          <div className="vehicle-bike">
            <div className="bike-wheel one"></div>
            <div className="bike-wheel two"></div>
          </div>
        ) : (
          <div className="vehicle-car">

            <div className="car-window"></div>
            <div className="car-window two"></div>

            <div className="car-wheel one"></div>
            <div className="car-wheel two"></div>

          </div>
        )}

      </div>

      {/* Ride information */}
      <div className="ride">

        <div>

          <div className="route">
            {ride.source} &rarr; {ride.destination}

            {ride.status !== 'SCHEDULED' && (
              <span
                className={`badge badge-${ride.status.toLowerCase()} ml`}
              >
                {ride.status}
              </span>
            )}
          </div>

          <div className="muted">
            {ride.rideDate} &middot; {ride.seats} seat(s) left &middot;
            Driver: {ride.driverName}{' '}

            {ride.driverRating > 0
              ? `(rating ${ride.driverRating})`
              : '(new driver)'}
          </div>

          <div className="muted">
            Vehicle: {ride.vehicle}
          </div>

          <div className="tags">

            {ride.ladiesOnly && (
              <span className="tag">Ladies only</span>
            )}

            {ride.noSmoking && (
              <span className="tag">No smoking</span>
            )}

            {ride.petsAllowed && (
              <span className="tag">Pets allowed</span>
            )}

          </div>

        </div>

        {/* Price + Request button */}
        <div className="ride-right">

          <div className="price">
            &#8377;{ride.price}
            <span className="muted"> /seat</span>
          </div>

          {action}

        </div>

      </div>

      {/* Additional content */}
      {children}

    </div>
  );
}

