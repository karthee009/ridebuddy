package com.ridebuddy.repository;

import com.ridebuddy.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserEmailOrderByIdDesc(String email);
    List<Notification> findByUserEmailAndSeenFalse(String email);
}
