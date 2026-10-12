package com.ridebuddy.service;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.model.*;
import com.ridebuddy.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class RideService {
    private final RideRepository rides;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final ReviewRepository reviews;
    private final ReviewService reviewService;
    private final NotificationService notifications;

    public RideService(RideRepository rides, BookingRepository bookings, UserRepository users,
                       VehicleRepository vehicles, ReviewRepository reviews,
                       ReviewService reviewService, NotificationService notifications) {
        this.rides = rides; this.bookings = bookings; this.users = users; this.vehicles = vehicles;
        this.reviews = reviews; this.reviewService = reviewService; this.notifications = notifications;
    }

    // ===================== Rides =====================

    /** Only upcoming, SCHEDULED rides are shown. Filters: date, ladies only. Sort: date or price. */
    public List<RideDto> search(String from, String to, LocalDate date, boolean ladiesOnly, String sort) {
        Comparator<Ride> order = "price".equals(sort)
                ? Comparator.comparingInt(Ride::getPrice)
                : Comparator.comparing(Ride::getRideDate);
        return rides.findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCaseAndStatusAndRideDateGreaterThanEqual(
                        from, to, RideStatus.SCHEDULED, LocalDate.now())
                .stream()
                .filter(r -> date == null || r.getRideDate().equals(date))
                .filter(r -> !ladiesOnly || r.isLadiesOnly())
                .sorted(order)
                .map(this::toDto)
                .toList();
    }

    public RideDto create(String email, RideRequest req) {
        User driver = userByEmail(email);
        Vehicle v = vehicles.findById(req.vehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!v.getOwner().getId().equals(driver.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your vehicle");
        int maxSeats = v.getTotalSeats() - 1; // the driver needs one seat
        if (req.seats() > maxSeats)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This vehicle can offer at most " + maxSeats + " seat(s)");

        Ride r = new Ride();
        r.setDriver(driver);
        r.setVehicle(v);
        r.setSource(req.source().trim());
        r.setDestination(req.destination().trim());
        r.setRideDate(req.rideDate());
        r.setPrice(req.price());
        r.setSeats(req.seats());
        r.setStatus(RideStatus.SCHEDULED);
        r.setLadiesOnly(req.ladiesOnly());
        r.setNoSmoking(req.noSmoking());
        r.setPetsAllowed(req.petsAllowed());
        return toDto(rides.save(r));
    }

    public List<RideDto> myRides(String email) {
        return rides.findByDriverEmailOrderByRideDateDesc(email).stream().map(this::toDto).toList();
    }

    @Transactional
    public RideDto start(String email, Long rideId) {
        Ride r = ownRide(email, rideId);
        if (r.getStatus() != RideStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a scheduled ride can be started");
        r.setStatus(RideStatus.STARTED);
        notifyPassengers(r, BookingStatus.ACCEPTED, "Your trip " + route(r) + " has started");
        return toDto(r);
    }

    @Transactional
    public RideDto complete(String email, Long rideId) {
        Ride r = ownRide(email, rideId);
        if (r.getStatus() != RideStatus.STARTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a started ride can be completed");
        r.setStatus(RideStatus.COMPLETED);
        notifyPassengers(r, BookingStatus.ACCEPTED, "Trip " + route(r) + " is completed. Please rate your driver");
        return toDto(r);
    }

    @Transactional
    public RideDto cancelRide(String email, Long rideId) {
        Ride r = ownRide(email, rideId);
        if (r.getStatus() != RideStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a scheduled ride can be cancelled");
        r.setStatus(RideStatus.CANCELLED);
        for (Booking b : bookings.findByRide(r)) {
            if (b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.ACCEPTED) {
                b.setStatus(BookingStatus.CANCELLED);
                notifications.send(b.getPassenger(), "The driver cancelled the ride " + route(r) + " on " + r.getRideDate());
            }
        }
        return toDto(r);
    }

    // ===================== Bookings =====================

    /** Passenger sends a request for N seats. Seats are reduced only when the driver accepts. */
    @Transactional
    public BookingDto book(String email, Long rideId, int seatsWanted) {
        User passenger = userByEmail(email);
        Ride ride = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This ride is not open for booking");
        if (ride.getDriver().getId().equals(passenger.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot book your own ride");
        if (bookings.existsByRideAndPassenger(ride, passenger))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already requested this ride");
        if (seatsWanted > ride.getSeats())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only " + ride.getSeats() + " seat(s) left");

        Booking b = new Booking();
        b.setRide(ride);
        b.setPassenger(passenger);
        b.setSeatsBooked(seatsWanted);
        b.setStatus(BookingStatus.PENDING);
        bookings.save(b);
        notifications.send(ride.getDriver(), passenger.getName() + " requested " + seatsWanted + " seat(s) on " + route(ride));
        return toBookingDto(b, passenger);
    }

    public List<BookingDto> myBookings(String email) {
        User me = userByEmail(email);
        return bookings.findByPassengerEmailOrderByIdDesc(email).stream().map(b -> toBookingDto(b, me)).toList();
    }

    public List<BookingDto> requestsForMyRides(String email) {
        User me = userByEmail(email);
        return bookings.findByRideDriverEmailOrderByIdDesc(email).stream().map(b -> toBookingDto(b, me)).toList();
    }

    @Transactional
    public BookingDto accept(String email, Long bookingId) {
        Booking b = pendingBookingOfDriver(email, bookingId);
        Ride ride = b.getRide();
        if (ride.getStatus() != RideStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This ride is no longer open");
        if (ride.getSeats() < b.getSeatsBooked())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough seats left to accept this request");
        ride.setSeats(ride.getSeats() - b.getSeatsBooked());
        b.setStatus(BookingStatus.ACCEPTED);
        notifications.send(b.getPassenger(), "Your request for " + route(ride) + " was ACCEPTED");
        return toBookingDto(b, userByEmail(email));
    }

    @Transactional
    public BookingDto decline(String email, Long bookingId, String reason) {
        Booking b = pendingBookingOfDriver(email, bookingId);
        b.setStatus(BookingStatus.DECLINED);
        b.setDeclineReason(reason.trim());
        notifications.send(b.getPassenger(), "Your request for " + route(b.getRide()) + " was declined: " + reason.trim());
        return toBookingDto(b, userByEmail(email));
    }

    /** Passenger cancels own request. If it was already accepted, the seats go back to the ride. */
    @Transactional
    public BookingDto cancelBooking(String email, Long bookingId) {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!b.getPassenger().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your booking");
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.ACCEPTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This request cannot be cancelled");
        Ride ride = b.getRide();
        if (ride.getStatus() != RideStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can cancel only before the trip starts");
        if (b.getStatus() == BookingStatus.ACCEPTED)
            ride.setSeats(ride.getSeats() + b.getSeatsBooked());
        b.setStatus(BookingStatus.CANCELLED);
        notifications.send(ride.getDriver(), b.getPassenger().getName() + " cancelled the request for " + route(ride));
        return toBookingDto(b, userByEmail(email));
    }

    // ===================== helpers =====================

    private Booking pendingBookingOfDriver(String email, Long bookingId) {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!b.getRide().getDriver().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the driver of this ride can do this");
        if (b.getStatus() != BookingStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This request is already " + b.getStatus());
        return b;
    }

    private Ride ownRide(String email, Long rideId) {
        Ride r = rides.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (!r.getDriver().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the driver of this ride can do this");
        return r;
    }

    private void notifyPassengers(Ride ride, BookingStatus status, String message) {
        for (Booking b : bookings.findByRide(ride))
            if (b.getStatus() == status) notifications.send(b.getPassenger(), message);
    }

    private String route(Ride r) { return r.getSource() + " to " + r.getDestination(); }

    private RideDto toDto(Ride r) {
        return RideDto.from(r, reviewService.avgRating(r.getDriver().getId()));
    }

    private BookingDto toBookingDto(Booking b, User viewer) {
        Ride r = b.getRide();
        boolean accepted = b.getStatus() == BookingStatus.ACCEPTED;
        return new BookingDto(b.getId(), toDto(r), b.getPassenger().getId(), b.getPassenger().getName(),
                b.getSeatsBooked(), b.getStatus().name(), b.getDeclineReason(),
                accepted ? r.getDriver().getPhone() : null,
                accepted ? r.getVehicle().getVehicleNumber() : null,
                accepted ? b.getPassenger().getPhone() : null,
                reviews.existsByBookingAndReviewer(b, viewer));
    }

    private User userByEmail(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login"));
    }
}
