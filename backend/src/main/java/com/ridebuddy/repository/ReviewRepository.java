package com.ridebuddy.repository;

import com.ridebuddy.model.Booking;
import com.ridebuddy.model.Review;
import com.ridebuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingAndReviewer(Booking booking, User reviewer);
    List<Review> findByRevieweeIdOrderByIdDesc(Long userId);
    long countByRevieweeId(Long userId);

    @Query("select coalesce(avg(r.rating), 0.0) from Review r where r.reviewee.id = :id")
    double averageRating(@Param("id") Long id);
}
