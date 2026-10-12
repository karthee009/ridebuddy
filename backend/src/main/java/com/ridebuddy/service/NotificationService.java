package com.ridebuddy.service;

import com.ridebuddy.dto.Dtos.NotificationDto;
import com.ridebuddy.model.Notification;
import com.ridebuddy.model.User;
import com.ridebuddy.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public void send(User user, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        repo.save(n);
    }

    public List<NotificationDto> list(String email) {
        return repo.findTop30ByUserEmailOrderByIdDesc(email).stream().map(NotificationDto::from).toList();
    }

    @Transactional
    public void markAllRead(String email) {
        List<Notification> unread = repo.findByUserEmailAndSeenFalse(email);
        unread.forEach(n -> n.setSeen(true));
        repo.saveAll(unread);
    }
}
