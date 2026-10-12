package com.ridebuddy.controller;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.model.User;
import com.ridebuddy.repository.UserRepository;
import com.ridebuddy.service.NotificationService;
import com.ridebuddy.service.ReviewService;
import com.ridebuddy.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Profile, vehicles and notifications of the logged-in user. */
@RestController
@RequestMapping("/api")
public class AccountController {
    private final UserRepository users;
    private final ReviewService reviews;
    private final VehicleService vehicles;
    private final NotificationService notifications;

    public AccountController(UserRepository users, ReviewService reviews,
                             VehicleService vehicles, NotificationService notifications) {
        this.users = users; this.reviews = reviews; this.vehicles = vehicles; this.notifications = notifications;
    }

    // ---- profile ----
    @GetMapping("/profile")
    public UserDto profile(Authentication auth) { return toDto(currentUser(auth)); }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody ProfileRequest req, Authentication auth) {
        User u = currentUser(auth);
        u.setName(req.name().trim());
        u.setPhone(req.phone() == null ? "" : req.phone().trim());
        users.save(u);
        return toDto(u);
    }

    // ---- vehicles ----
    @GetMapping("/vehicles")
    public List<VehicleDto> myVehicles(Authentication auth) { return vehicles.list(auth.getName()); }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleDto> addVehicle(@Valid @RequestBody VehicleRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicles.add(auth.getName(), req));
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id, Authentication auth) {
        vehicles.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- notifications ----
    @GetMapping("/notifications")
    public List<NotificationDto> myNotifications(Authentication auth) { return notifications.list(auth.getName()); }

    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markRead(Authentication auth) {
        notifications.markAllRead(auth.getName());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(); }

    private UserDto toDto(User u) { return UserDto.of(u, reviews.avgRating(u.getId()), reviews.count(u.getId())); }
}
