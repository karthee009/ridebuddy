package com.ridebuddy.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rides")
public class Ride {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User driver;

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    private String source;

    private String destination;

    private LocalDate rideDate;

    private int price;

    private int seats;

    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.SCHEDULED;

    private boolean ladiesOnly;

    private boolean noSmoking;

    private boolean petsAllowed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getRideDate() { return rideDate; }
    public void setRideDate(LocalDate rideDate) { this.rideDate = rideDate; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }

    public boolean isLadiesOnly() { return ladiesOnly; }
    public void setLadiesOnly(boolean ladiesOnly) { this.ladiesOnly = ladiesOnly; }

    public boolean isNoSmoking() { return noSmoking; }
    public void setNoSmoking(boolean noSmoking) { this.noSmoking = noSmoking; }

    public boolean isPetsAllowed() { return petsAllowed; }
    public void setPetsAllowed(boolean petsAllowed) { this.petsAllowed = petsAllowed; }
}
