package com.ridebuddy.controller;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(auth.getName(), req));
    }

    @GetMapping("/users/{id}/reviews")
    public List<ReviewDto> forUser(@PathVariable Long id) { return service.forUser(id); }
}
