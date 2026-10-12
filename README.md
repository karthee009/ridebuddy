# RideBuddy - Carpooling Web Application

**Stack:** React (Vite) + Java Spring Boot + Spring Security (session based authentication) + MySQL / H2
**Theme:** White and Blue

---

## 1. Project Overview
RideBuddy is a carpooling (ride-sharing) web application inspired by BlaBlaCar. Drivers who travel between two places register their vehicle and offer empty seats. Passengers search rides, send seat requests, and the driver accepts or declines. After the trip, both sides rate each other. The fuel and toll cost is shared, so travel becomes cheaper and there are fewer vehicles on the road.

## 2. Problem Statement
People travel the same routes every day (for example Tiruppur to Coimbatore or Chennai) in private cars with empty seats, while others pay more for bus, train or taxi. There is no simple, trusted platform that connects them.

## 3. Objectives
1. Secure registration and login using session based authentication.
2. Let drivers register vehicles and offer rides with preferences.
3. Let passengers search, filter and request seats.
4. Let drivers accept or decline requests (reason compulsory when declining).
5. Manage the full trip life cycle: Scheduled, Started, Completed or Cancelled.
6. Build trust using ratings, reviews, vehicle details and contact sharing after acceptance.
7. Keep users informed using in-app notifications.

## 4. Features
| Module | Feature |
|---|---|
| Authentication | Register, Login, Logout; session based; BCrypt password hashing |
| Profile | Edit name and phone; see own average rating and reviews |
| Vehicles | Add / list / delete vehicles (type, brand, model, colour, number, seats, AC, fuel) |
| Rides | Offer a ride with a selected vehicle; preferences: Ladies only, No smoking, Pets allowed |
| Search | Filter by From, To, Date, Ladies only; sort by Date or Price; past and non-scheduled rides are hidden |
| Booking | Request 1 or more seats; status Pending / Accepted / Declined / Cancelled |
| Driver approval | Accept, or Decline with a mandatory reason |
| Cancellation | Driver cancels a ride (all requests cancelled); passenger cancels own request (seats return if it was accepted) |
| Trip status | Driver: Start trip, Complete trip |
| Ratings | After completion, driver and passenger rate each other (1-5 stars + comment) once |
| Notifications | In-app alerts with unread counter (request received, accepted, declined, cancelled, trip started, completed, rated) |
| Privacy | Vehicle number and phone numbers are shown only after the request is Accepted |

## 5. Technologies Used
| Layer | Technology |
|---|---|
| Frontend | React 18, React Router, Vite, plain CSS |
| Backend | Java 17+ (runs on JDK 25), Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation |
| Database | MySQL (or H2 file database) |
| Build tools | Maven, npm |

## 6. System Architecture
```
 Browser (React, port 5173)
        |   fetch('/api/...', credentials: include)   + JSESSIONID cookie
        v
 Vite dev proxy  -->  Spring Boot (port 8080)
                         |-- Controller   (HTTP requests, validation)
                         |-- Service      (business rules)
                         |-- Repository   (Spring Data JPA)
                         v
                      Database (MySQL / H2)
```

## 7. Session Based Authentication
**Why session and not JWT?** The server remembers the logged-in user. The browser only holds a random id (`JSESSIONID` cookie). Nothing sensitive is stored in the browser and logout really ends the login on the server.

1. **Register:** password is hashed with BCrypt and saved.
2. **Login:** `POST /api/auth/login` -> `AuthenticationManager` verifies email and password using `CustomUserDetailsService`.
3. On success the user is stored in the `HttpSession`; the server sends `Set-Cookie: JSESSIONID=...`.
4. The browser sends this cookie with every request; Spring Security identifies the user.
5. No valid session -> `401`; React redirects to the login page.
6. **Logout:** `POST /api/auth/logout` invalidates the session.
7. **Refresh:** React calls `GET /api/auth/me`; a valid cookie keeps the user logged in.

The Vite proxy forwards `/api` to port 8080, so the browser sees one origin and the cookie works without CORS settings.

## 8. Trip and Booking Life Cycles

**Ride status:** `SCHEDULED -> STARTED -> COMPLETED`, or `SCHEDULED -> CANCELLED`
**Booking status:** `PENDING -> ACCEPTED`, `PENDING -> DECLINED (reason)`, `PENDING/ACCEPTED -> CANCELLED`

Flow:
1. Driver adds a vehicle, then offers a ride (seats must be at most vehicle seats minus 1 for the driver).
2. Passenger requests N seats -> booking `PENDING`; driver gets a notification. Seats are **not** reduced yet.
3. Driver **accepts** -> seats reduce by N, status `ACCEPTED`, passenger is notified and now sees driver phone and vehicle number.
4. Driver **declines** -> must enter a reason; passenger is notified with the reason.
5. Passenger may cancel a Pending/Accepted request before the trip starts (accepted seats return to the ride).
6. Driver may cancel a Scheduled ride; every Pending/Accepted booking becomes Cancelled and passengers are notified.
7. Driver presses **Start trip** then **Complete trip**; passengers are notified.
8. After completion, each side can rate the other once for that booking.

## 9. Database Design
| Table | Columns |
|---|---|
| users | id, name, email (unique), password (BCrypt), phone |
| vehicles | id, owner_id -> users, vehicle_type, brand, model, color, vehicle_number, total_seats, ac, fuel_type |
| rides | id, driver_id -> users, vehicle_id -> vehicles, source, destination, ride_date, price, seats (available), status, ladies_only, no_smoking, pets_allowed |
| bookings | id, ride_id -> rides, passenger_id -> users, seats_booked, status, decline_reason; unique (ride_id, passenger_id) |
| reviews | id, booking_id -> bookings, reviewer_id -> users, reviewee_id -> users, rating, comment; unique (booking_id, reviewer_id) |
| notifications | id, user_id -> users, message, seen, created_at |

Relationships: user 1-N vehicles; user 1-N rides; vehicle 1-N rides; ride 1-N bookings; booking 1-N reviews (max 2, one from each side); user 1-N notifications.

## 10. REST API
| Method | URL | Login | Description |
|---|---|---|---|
| POST | /api/auth/register | No | Create account |
| POST | /api/auth/login | No | Login (creates session) |
| POST | /api/auth/logout | No | End session |
| GET | /api/auth/me | Yes | Current user |
| GET / PUT | /api/profile | Yes | View / update name and phone, with rating |
| GET | /api/vehicles | Yes | My vehicles |
| POST | /api/vehicles | Yes | Add vehicle |
| DELETE | /api/vehicles/{id} | Yes | Delete vehicle (not allowed if used in a ride) |
| GET | /api/rides?from=&to=&date=&ladiesOnly=&sort= | No | Search upcoming rides (`sort` = date or price) |
| POST | /api/rides | Yes | Offer a ride |
| GET | /api/rides/mine | Yes | Rides I offered |
| POST | /api/rides/{id}/start | Yes (driver) | Start trip |
| POST | /api/rides/{id}/complete | Yes (driver) | Complete trip |
| POST | /api/rides/{id}/cancel | Yes (driver) | Cancel ride |
| POST | /api/rides/{id}/book | Yes | Request seats, body `{"seats": 2}` |
| GET | /api/bookings/mine | Yes | My requests |
| GET | /api/bookings/requests | Yes | Requests for my rides |
| POST | /api/bookings/{id}/accept | Yes (driver) | Accept |
| POST | /api/bookings/{id}/decline | Yes (driver) | Decline, body `{"reason": "..."}` |
| POST | /api/bookings/{id}/cancel | Yes (passenger) | Cancel my request |
| POST | /api/reviews | Yes | Rate a completed trip |
| GET | /api/users/{id}/reviews | Yes | Reviews about a user |
| GET | /api/notifications | Yes | My latest 30 notifications |
| POST | /api/notifications/read | Yes | Mark all as read |

Errors are returned as `{ "message": "..." }`.

## 11. Project Structure
```
ridebuddy-fullstack/
|-- backend/
|   |-- pom.xml
|   `-- src/main/
|       |-- resources/application.properties
|       `-- java/com/ridebuddy/
|           |-- RideBuddyApplication.java
|           |-- model/        User, Vehicle, Ride, Booking, Review, Notification, RideStatus, BookingStatus
|           |-- repository/   one repository per entity
|           |-- dto/          Dtos (request/response records)
|           |-- security/     SecurityConfig, CustomUserDetailsService
|           |-- service/      RideService, VehicleService, ReviewService, NotificationService
|           `-- controller/   AuthController, AccountController, RideController, ReviewController, GlobalExceptionHandler
`-- frontend/
    |-- package.json, vite.config.js, index.html
    `-- src/
        |-- main.jsx, App.jsx, api.js, AuthContext.jsx, styles.css
        |-- components/  RideCard.jsx
        `-- pages/       Auth, Search, PostRide, MyTrips, Vehicles, Profile, Notifications
```

## 12. How to Run
**Requirements:** JDK 17 or higher (tested setup: JDK 25), Maven 3.9+, Node.js 18 or higher (tested setup: Node 24), MySQL (optional).

**Backend**
```
cd backend
mvn spring-boot:run
```
Runs on http://localhost:8080 .

**Frontend** (new terminal)
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173

**Database**
- Default is H2 (file `backend/data/ridebuddy`). Console: http://localhost:8080/h2-console
- MySQL: add `mysql-connector-j` to `pom.xml` and set in `application.properties`:
```
spring.datasource.url=jdbc:mysql://localhost:3306/ridebuddy?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```
- **After upgrading from an older version** the table structure changed. Reset the database once: in MySQL run `DROP DATABASE ridebuddy; CREATE DATABASE ridebuddy;` (for H2 delete the `backend/data` folder), then restart the backend.

**Testing with two users:** use a normal window for the driver and an incognito window for the passenger (the session cookie is per browser).

## 13. Test Cases
| # | Scenario | Expected result |
|---|---|---|
| 1 | Register with new email | Account created and auto login |
| 2 | Register with existing email | "Email already registered" |
| 3 | Password under 6 characters | Validation message |
| 4 | Login with wrong password | "Invalid email or password" |
| 5 | Open /post without login | Redirected to login |
| 6 | Offer a ride without any vehicle | Page asks to add a vehicle first |
| 7 | Add vehicle, then offer ride with seats above (vehicle seats - 1) | Error message |
| 8 | Offer ride with a past date | Validation error |
| 9 | Search with From/To, date, ladies only, sort by price | Correct filtered and sorted list |
| 10 | Passenger requests 2 seats | Status PENDING; seats unchanged; driver gets notification |
| 11 | Request more seats than available | "Only N seat(s) left" |
| 12 | Request the same ride twice / own ride | Error messages |
| 13 | Driver accepts | ACCEPTED; seats reduce by 2; passenger sees phone and vehicle number |
| 14 | Driver declines with empty reason | "Please enter a reason for declining" |
| 15 | Driver declines with reason | DECLINED; passenger sees the reason |
| 16 | Passenger cancels accepted request | CANCELLED; seats return to the ride |
| 17 | Driver cancels a ride | Ride CANCELLED; all requests CANCELLED; passengers notified |
| 18 | Start trip, then Complete trip | Status changes; passengers notified |
| 19 | Rate before completion | "You can rate only after the trip is completed" |
| 20 | Rate after completion (both sides), rate again | First succeeds; second "You already rated this trip" |
| 21 | Another user calls accept/decline/cancel on someone else's data | 403 Forbidden |
| 22 | Delete vehicle used in a ride | Error; unused vehicle is deleted |
| 23 | Update profile with 9-digit phone | Validation message |
| 24 | Logout then call /api/auth/me | 401 Unauthorized |

## 14. Security Notes
- BCrypt password hashing; DTOs never expose the password hash.
- Session cookie is `HttpOnly`.
- Authorization is checked on the server (only the driver can accept, decline, start, complete or cancel; only the passenger can cancel own request).
- Vehicle number and phone numbers are revealed only after acceptance.
- **CSRF protection is disabled** to keep the learning project simple. In production enable CSRF tokens, use HTTPS and a `Secure` cookie.
- The H2 console is for development only.

## 15. Limitations
- Real maps, online payment, intermediate stops, chat, admin panel and licence verification are not included.
- A trip can be started on any day (no date check) so it is easy to demonstrate.
- No ride editing (cancel and create a new ride instead) and no pagination (suitable for small data).
- "Ladies only" is a label chosen by the driver; gender is not stored or verified.

## 16. Future Enhancements
1. Google Maps / OpenStreetMap route and automatic price suggestion by distance.
2. Intermediate stops (pick-up points along the route).
3. Real-time chat between driver and passenger.
4. UPI payment integration.
5. Admin panel (manage users, rides, complaints).
6. Driver verification (licence and RC upload).
7. SOS button and trip sharing.
8. Email / SMS / push notifications, ride editing, pagination.
9. Enable CSRF protection and deploy with HTTPS.

## 17. Viva Questions and Answers
- **What is carpooling?** Sharing a private vehicle on a common route and splitting the cost.
- **What is session based authentication?** The server stores login state in an HttpSession and gives the browser a session id cookie.
- **Session vs JWT?** Session: state on server, easy to revoke, cookie holds only an id. JWT: state in a signed token on the client, server is stateless.
- **Why BCrypt?** It is slow and salted, so stored passwords are hard to crack.
- **What does `UserDetailsService` do?** Loads the user from the database for Spring Security during login.
- **What is a DTO and why use it?** A plain object for requests/responses; it avoids exposing entities such as the password hash.
- **Why `@Transactional` on accept?** Seat reduction and status change succeed or fail together.
- **Why are seats reduced on accept and not on request?** A request is only a wish; seats change only when the driver confirms.
- **How do you make sure only the driver can accept?** The service compares the logged-in email (from the session) with the ride's driver email; otherwise 403.
- **Why hide vehicle number and phone until accepted?** Privacy and safety; only confirmed travellers need them.
- **How are ratings calculated?** Average of `rating` in `reviews` for that user (JPQL `avg`), rounded to 1 decimal.
- **Why one review per booking per person?** Unique constraint (booking_id, reviewer_id) prevents duplicate or fake ratings.
- **What is JPA?** Java Persistence API; maps Java classes to database tables.
- **What does `credentials: 'include'` do?** Tells fetch to send cookies with the request.
- **Why the Vite proxy?** Same origin for the browser, so cookies work without CORS.

## 18. Conclusion
RideBuddy is a complete full-stack carpooling application with secure session login, vehicle management, a booking approval workflow, trip life cycle, ratings and notifications. It can be extended step by step with maps, payments and chat.
