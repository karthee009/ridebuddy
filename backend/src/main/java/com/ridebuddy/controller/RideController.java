package com.ridebuddy.controller;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.service.RideService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RideController {
    private final RideService service;

    public RideController(RideService service) { this.service = service; }

    // ---- rides ----
    @GetMapping("/rides")
    public List<RideDto> search(@RequestParam(defaultValue = "") String from,
                                @RequestParam(defaultValue = "") String to,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam(defaultValue = "false") boolean ladiesOnly,
                                @RequestParam(defaultValue = "date") String sort) {
        return service.search(from.trim(), to.trim(), date, ladiesOnly, sort);
    }

    @PostMapping("/rides")
    public ResponseEntity<RideDto> create(@Valid @RequestBody RideRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(auth.getName(), req));
    }

    @GetMapping("/rides/mine")
    public List<RideDto> myRides(Authentication auth) { return service.myRides(auth.getName()); }

    @PostMapping("/rides/{id}/start")
    public RideDto start(@PathVariable Long id, Authentication auth) { return service.start(auth.getName(), id); }

    @PostMapping("/rides/{id}/complete")
    public RideDto complete(@PathVariable Long id, Authentication auth) { return service.complete(auth.getName(), id); }

    @PostMapping("/rides/{id}/cancel")
    public RideDto cancelRide(@PathVariable Long id, Authentication auth) { return service.cancelRide(auth.getName(), id); }

    // ---- bookings ----
    @PostMapping("/rides/{id}/book")
    public BookingDto book(@PathVariable Long id, @Valid @RequestBody BookRequest req, Authentication auth) {
        return service.book(auth.getName(), id, req.seats());
    }

    @GetMapping("/bookings/mine")
    public List<BookingDto> myBookings(Authentication auth) { return service.myBookings(auth.getName()); }

    @GetMapping("/bookings/requests")
    public List<BookingDto> requests(Authentication auth) { return service.requestsForMyRides(auth.getName()); }

    @PostMapping("/bookings/{id}/accept")
    public BookingDto accept(@PathVariable Long id, Authentication auth) { return service.accept(auth.getName(), id); }

    @PostMapping("/bookings/{id}/decline")
    public BookingDto decline(@PathVariable Long id, @Valid @RequestBody DeclineRequest req, Authentication auth) {
        return service.decline(auth.getName(), id, req.reason());
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingDto cancelBooking(@PathVariable Long id, Authentication auth) {
        return service.cancelBooking(auth.getName(), id);
    }
}
