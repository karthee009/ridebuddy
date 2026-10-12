package com.ridebuddy.service;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.model.User;
import com.ridebuddy.model.Vehicle;
import com.ridebuddy.repository.RideRepository;
import com.ridebuddy.repository.UserRepository;
import com.ridebuddy.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final RideRepository rides;

    public VehicleService(VehicleRepository vehicles, UserRepository users, RideRepository rides) {
        this.vehicles = vehicles; this.users = users; this.rides = rides;
    }

    public List<VehicleDto> list(String email) {
        return vehicles.findByOwnerEmailOrderByIdDesc(email).stream().map(VehicleDto::from).toList();
    }

    public VehicleDto add(String email, VehicleRequest req) {
        User owner = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login"));
        Vehicle v = new Vehicle();
        v.setOwner(owner);
        v.setVehicleType(req.vehicleType());
        v.setBrand(req.brand().trim());
        v.setModel(req.model().trim());
        v.setColor(req.color().trim());
        v.setVehicleNumber(req.vehicleNumber().trim().toUpperCase());
        v.setTotalSeats(req.totalSeats());
        v.setAc(req.ac());
        v.setFuelType(req.fuelType());
        return VehicleDto.from(vehicles.save(v));
    }

    public void delete(String email, Long id) {
        Vehicle v = vehicles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!v.getOwner().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your vehicle");
        if (rides.existsByVehicle(v))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This vehicle is used in a ride, so it cannot be deleted");
        vehicles.delete(v);
    }
}
