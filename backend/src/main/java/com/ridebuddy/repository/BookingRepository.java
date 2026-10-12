package com.ridebuddy.repository;

import com.ridebuddy.model.Booking;
import com.ridebuddy.model.Ride;
import com.ridebuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRideAndPassenger(Ride ride, User passenger);
    List<Booking> findByRide(Ride ride);
    List<Booking> findByPassengerEmailOrderByIdDesc(String email);
    List<Booking> findByRideDriverEmailOrderByIdDesc(String email);
}
