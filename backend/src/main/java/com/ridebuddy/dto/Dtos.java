package com.ridebuddy.dto;

import com.ridebuddy.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**  PASSWROD HASH VEHICLE ELLATHUKUM CONTROL IDHUTHAAN */
public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record ProfileRequest(
            @NotBlank String name,
            @Pattern(regexp = "^$|^[0-9]{10}$", message = "must be a 10 digit number") String phone) {}

    public record UserDto(Long id, String name, String email, String phone, double avgRating, long ratingCount) {
        public static UserDto basic(User u) { return of(u, 0, 0); }
        public static UserDto of(User u, double avg, long count) {
            return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), avg, count);
        }
    }

    // ----- vehicles -----
    public record VehicleRequest(
            @NotBlank String vehicleType, @NotBlank String brand, @NotBlank String model, @NotBlank String color,
            @NotBlank @Size(max = 15) String vehicleNumber,
            @Min(1) @Max(8) int totalSeats, boolean ac, @NotBlank String fuelType) {}

    public record VehicleDto(Long id, String vehicleType, String brand, String model, String color,
                             String vehicleNumber, int totalSeats, boolean ac, String fuelType) {
        public static VehicleDto from(Vehicle v) {
            return new VehicleDto(v.getId(), v.getVehicleType(), v.getBrand(), v.getModel(), v.getColor(),
                    v.getVehicleNumber(), v.getTotalSeats(), v.isAc(), v.getFuelType());
        }
    }

    public record RideRequest(
            @NotBlank String source,
            @NotBlank String destination,
            @NotNull @FutureOrPresent LocalDate rideDate,
            @Min(1) int price,
            @Min(1) @Max(7) int seats,
            @NotNull Long vehicleId,
            boolean ladiesOnly, boolean noSmoking, boolean petsAllowed) {}

    public record RideDto(Long id, Long driverId, String driverName, double driverRating,
                          String source, String destination, LocalDate rideDate, int price, int seats,
                          String status, boolean ladiesOnly, boolean noSmoking, boolean petsAllowed, String vehicle) {
        public static RideDto from(Ride r, double driverRating) {
            Vehicle v = r.getVehicle();
            String vehicle = v.getColor() + " " + v.getBrand() + " " + v.getModel()
                    + " (" + v.getVehicleType() + (v.isAc() ? ", AC" : "") + ", " + v.getFuelType() + ")";
            return new RideDto(r.getId(), r.getDriver().getId(), r.getDriver().getName(), driverRating,
                    r.getSource(), r.getDestination(), r.getRideDate(), r.getPrice(), r.getSeats(),
                    r.getStatus().name(), r.isLadiesOnly(), r.isNoSmoking(), r.isPetsAllowed(), vehicle);
        }
    }

    // ----- bookings -----
    public record BookRequest(@Min(1) @Max(7) int seats) {}

    public record DeclineRequest(
            @NotBlank(message = "is required") @Size(max = 255, message = "is too long (max 255)") String reason) {}

    /** driverPhone, vehicleNumber and passengerPhone are filled only after the driver ACCEPTS the request. */
    public record BookingDto(Long id, RideDto ride, Long passengerId, String passengerName, int seats,
                             String status, String declineReason,
                             String driverPhone, String vehicleNumber, String passengerPhone, boolean reviewedByMe) {}

    // ----- reviews / notifications -----
    public record ReviewRequest(@NotNull Long bookingId, @Min(1) @Max(5) int rating, @Size(max = 255) String comment) {}

    public record ReviewDto(Long id, String reviewerName, int rating, String comment) {
        public static ReviewDto from(Review r) {
            return new ReviewDto(r.getId(), r.getReviewer().getName(), r.getRating(), r.getComment());
        }
    }

    public record NotificationDto(Long id, String message, boolean seen, String createdAt) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getMessage(), n.isSeen(), n.getCreatedAt().toString());
        }
    }
}
