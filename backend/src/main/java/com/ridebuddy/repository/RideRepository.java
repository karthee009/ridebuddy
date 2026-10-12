package com.ridebuddy.repository;

import com.ridebuddy.model.Ride;
import com.ridebuddy.model.RideStatus;
import com.ridebuddy.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCaseAndStatusAndRideDateGreaterThanEqual(
            String source, String destination, RideStatus status, LocalDate fromDate);
    List<Ride> findByDriverEmailOrderByRideDateDesc(String email);
    boolean existsByVehicle(Vehicle vehicle);
}
