package com.ridebuddy.service;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.model.*;
import com.ridebuddy.repository.BookingRepository;
import com.ridebuddy.repository.ReviewRepository;
import com.ridebuddy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final NotificationService notifications;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings,
                         UserRepository users, NotificationService notifications) {
        this.reviews = reviews; this.bookings = bookings; this.users = users; this.notifications = notifications;
    }

    /** Driver and passenger of an accepted booking can rate each other once the trip is COMPLETED. */
    @Transactional
    public ReviewDto create(String email, ReviewRequest req) {
        User me = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login"));
        Booking b = bookings.findById(req.bookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (b.getStatus() != BookingStatus.ACCEPTED || b.getRide().getStatus() != RideStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can rate only after the trip is completed");

        User driver = b.getRide().getDriver();
        User passenger = b.getPassenger();
        User reviewee;
        if (me.getId().equals(passenger.getId())) reviewee = driver;
        else if (me.getId().equals(driver.getId())) reviewee = passenger;
        else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You were not part of this trip");

        if (reviews.existsByBookingAndReviewer(b, me))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already rated this trip");

        Review r = new Review();
        r.setBooking(b);
        r.setReviewer(me);
        r.setReviewee(reviewee);
        r.setRating(req.rating());
        r.setComment(req.comment() == null ? "" : req.comment().trim());
        reviews.save(r);
        notifications.send(reviewee, me.getName() + " gave you " + req.rating() + " star(s)");
        return ReviewDto.from(r);
    }

    public List<ReviewDto> forUser(Long userId) {
        return reviews.findByRevieweeIdOrderByIdDesc(userId).stream().map(ReviewDto::from).toList();
    }

    public double avgRating(Long userId) {
        return Math.round(reviews.averageRating(userId) * 10) / 10.0;
    }

    public long count(Long userId) { return reviews.countByRevieweeId(userId); }
}
