package com.ridebuddy.controller;

import com.ridebuddy.dto.Dtos.*;
import com.ridebuddy.model.User;
import com.ridebuddy.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;

    public AuthController(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager) {
        this.users = users; this.encoder = encoder; this.authManager = authManager;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password())); // hash the password
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.basic(u));
    }

    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody LoginRequest req,
                         HttpServletRequest request, HttpServletResponse response) {
        Authentication auth;
        try {
            auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    req.email().trim().toLowerCase(), req.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        // Store the logged-in user in the HTTP session. The browser gets a JSESSIONID cookie.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        return UserDto.basic(users.findByEmail(auth.getName()).orElseThrow());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate(); // destroys the session on the server
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me(Authentication auth) {
        return UserDto.basic(users.findByEmail(auth.getName()).orElseThrow());
    }
}
