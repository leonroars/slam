package com.slam.concertreservation.interfaces;

import com.slam.concertreservation.application.facade.ConcertReservationApplication;
import com.slam.concertreservation.application.facade.UserApplication;
import com.slam.concertreservation.component.idempotency.Idempotent;
import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.interfaces.dto.ConcertResponse;
import com.slam.concertreservation.interfaces.dto.ConcertScheduleResponse;
import com.slam.concertreservation.interfaces.dto.ReservationResponse;
import com.slam.concertreservation.interfaces.dto.SeatResponse;
import com.slam.concertreservation.interfaces.dto.TokenResponse;
import com.slam.concertreservation.interfaces.dto.UserPointBalanceResponse;
import com.slam.concertreservation.interfaces.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import org.springframework.web.bind.annotation.CookieValue;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConcertReservationController {

    private final ConcertReservationApplication reservationApp;
    private final UserApplication userApp;

    /**
     * Register a new user with the given name.
     *
     * @param name the name of the user to register
     * @return a ResponseEntity containing a UserResponse for the newly created user
     */

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestParam String name) {
        User user = userApp.registerUser(name);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Retrieve user information for the specified user ID.
     *
     * @param userId the user's numeric ID provided as a path variable
     * @return the user's details wrapped in a UserResponse
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(UserResponse.from(userApp.getUser(Long.valueOf(userId))));
    }

    /**
     * Retrieve a user's point balance.
     *
     * @param userId the user identifier as a decimal string
     * @return a {@link UserPointBalanceResponse} containing the user's current point balance
     */

    @GetMapping("/users/{userId}/point")
    public ResponseEntity<UserPointBalanceResponse> getUserPointBalance(@PathVariable String userId) {
        UserPointBalance balance = reservationApp.getUserPointBalance(Long.valueOf(userId));
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    /**
     * Add points to a user's account and return the updated point balance.
     *
     * @param userId the user's identifier as a string (will be parsed to a numeric ID)
     * @param amount the number of points to charge to the user's account
     * @return the user's updated point balance wrapped in a UserPointBalanceResponse
     */
    @PostMapping("/users/{userId}/point/charge")
    public ResponseEntity<UserPointBalanceResponse> chargeUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        UserPointBalance balance = reservationApp.chargeUserPoint(Long.valueOf(userId), amount);
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    /**
     * Deducts the specified amount of points from the user's account.
     *
     * @return the user's updated point balance as a UserPointBalanceResponse
     */
    @PostMapping("/users/{userId}/point/use")
    public ResponseEntity<UserPointBalanceResponse> useUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        UserPointBalance balance = reservationApp.useUserPoint(Long.valueOf(userId), amount);
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    /* ========== Concert ========== */

    /**
     * Register a new concert with the given name and artist.
     *
     * @param name   the concert's name
     * @param artist the performing artist's name
     * @return a ConcertResponse representing the newly registered concert
     */
    @PostMapping("/concerts")
    public ResponseEntity<ConcertResponse> registerConcert(
            @RequestParam String name,
            @RequestParam String artist) {
        Concert concert = reservationApp.registerConcert(name, artist);
        return ResponseEntity.ok(ConcertResponse.from(concert));
    }

    /**
     * Retrieve details for a concert by its identifier.
     *
     * @param concertId the identifier of the concert to retrieve
     * @return the concert information as a ConcertResponse
     */
    @GetMapping("/concerts/{concertId}")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        Concert concert = reservationApp.getConcert(concertId);
        return ResponseEntity.ok(ConcertResponse.from(concert));
    }

    /**
     * Registers a schedule for the specified concert.
     *
     * @param concertId         the ID of the concert to associate the schedule with
     * @param concertDateTime   the date and time of the concert performance
     * @param reservationStartAt the start date and time when reservations open
     * @param reservationEndAt   the end date and time when reservations close
     * @param price             the ticket price for the schedule
     * @return                  a ConcertScheduleResponse representing the created schedule and its associated concert
     */
    @PostMapping("/concerts/{concertId}/schedules")
    public ResponseEntity<ConcertScheduleResponse> registerConcertSchedule(
            @PathVariable Long concertId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime concertDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reservationStartAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reservationEndAt,
            @RequestParam int price) {
        ConcertSchedule schedule = reservationApp.registerConcertSchedule(
                concertId,
                concertDateTime,
                reservationStartAt,
                reservationEndAt,
                price);
        Concert concert = reservationApp.getConcert(schedule.getConcertId());
        return ResponseEntity.ok(ConcertScheduleResponse.from(schedule, concert));
    }

    /**
     * List available concert schedules.
     *
     * @return a list of ConcertScheduleResponse DTOs representing schedules available for reservation
     */
    @GetMapping("/concerts/schedules/available")
    public ResponseEntity<List<ConcertScheduleResponse>> getAvailableConcertSchedules() {
        List<ConcertSchedule> schedules = reservationApp.getAvailableConcertSchedules();
        List<ConcertScheduleResponse> responses = schedules.stream()
                .map(schedule -> {
                    Concert concert = reservationApp.getConcert(schedule.getConcertId());
                    return ConcertScheduleResponse.from(schedule, concert);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieve available seats for a given concert schedule.
     *
     * @param scheduleId the concert schedule ID to query available seats for
     * @return a list of SeatResponse objects representing seats that are currently available for reservation
     */
    @GetMapping("/concerts/schedules/{scheduleId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long scheduleId) {
        List<Seat> seats = reservationApp.getAvailableSeats(scheduleId);
        return ResponseEntity.ok(seats.stream().map(SeatResponse::from).toList());
    }

    /* ========== Reservation ========== */

    /**
     * Pre-reserves a specific seat for a user on a concert schedule.
     *
     * @param scheduleId the ID of the concert schedule
     * @param seatId     the ID of the seat to reserve
     * @param userId     the ID of the user who will hold the seat
     * @return           the `SeatResponse` representing the reserved seat
     */
    @PostMapping("/concerts/schedules/{scheduleId}/seats/{seatId}/assign")
    public ResponseEntity<SeatResponse> reserveSeat(@PathVariable Long scheduleId,
            @PathVariable Long seatId,
            @RequestParam Long userId) {
        Seat seat = reservationApp.assignSeat(scheduleId, userId, seatId);
        return ResponseEntity.ok(SeatResponse.from(seat));
    }

    /**
         * Create a temporary (preliminary) reservation for a user.
         *
         * @return the created reservation wrapped in a ReservationResponse, including the associated Seat information
         */
    @PostMapping("/reservations")
    @Idempotent(operationKey = "reservation.create")
    public ResponseEntity<ReservationResponse> createTemporaryReservation(
            @RequestParam Long userId,
            @RequestParam Long scheduleId,
            @RequestParam Long seatId,
            @RequestParam Integer price) {
        Reservation reservation = reservationApp.createTemporaryReservation(userId, scheduleId, seatId, price);
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity
                .ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * Confirm a reservation and process its payment.
     *
     * Confirms the reservation identified by `reservationId` by charging the specified `userId` the provided `price`
     * and returns the updated reservation along with its associated seat.
     *
     * @param reservationId the identifier of the reservation to confirm
     * @param userId        the identifier of the user who will be charged
     * @param price         the payment amount to process
     * @return              a `ReservationResponse` containing the confirmed reservation and its associated seat
     */
    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable String reservationId,
            @RequestParam String userId,
            @RequestParam Integer price) {
        Reservation reservation = reservationApp.paymentRequestForReservation(Long.valueOf(userId), price,
                Long.valueOf(reservationId));
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity
                .ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * Cancel a reservation identified by the given reservationId.
     *
     * @param reservationId the reservation identifier as a numeric string
     * @return the cancelled reservation wrapped as a ReservationResponse including its associated seat
     */
    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationApp.cancelReservation(Long.valueOf(reservationId));
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity.ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * Retrieve a reservation by its identifier and return its DTO including the associated seat.
     *
     * @param reservationId the reservation identifier as a string
     * @return a ReservationResponse containing the reservation details and its associated seat
     */
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationApp.getReservation(Long.valueOf(reservationId));
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity.ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * Retrieves all reservations for the specified user.
     *
     * @param userId the user's identifier as a string (will be converted to a long)
     * @return a list of ReservationResponse DTOs representing the user's reservations
     */
    @GetMapping("/users/{userId}/reservations")
    public ResponseEntity<List<ReservationResponse>> getUserReservations(@PathVariable String userId) {
        List<Reservation> reservations = reservationApp.getUserReservations(Long.valueOf(userId));
        List<ReservationResponse> responses = reservations.stream()
                .map(reservation -> {
                    // Seat 정보 조회하여 seatNumber 포함
                    Seat seat = reservationApp.getSeat(reservation.getSeatId());
                    return ReservationResponse.from(reservation, seat);
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    /* ========== Queue ========== */

    /**
     * Issue a queue token for a user and store the token ID in an HttpOnly cookie.
     *
     * The issued token's ID is added to an HttpOnly cookie named "tokenId" with path "/".
     *
     * @param userId     the user identifier as a string
     * @param scheduleId the identifier of the schedule for which the token is issued
     * @return           a TokenResponse containing the issued token
     */
    @PostMapping("/queue/tokens")
    public ResponseEntity<TokenResponse> issueToken(@RequestParam String userId,
            @RequestParam Long scheduleId,
            HttpServletResponse response) {
        Token token = reservationApp.issueToken(Long.valueOf(userId), scheduleId);
        Cookie cookie = new Cookie("tokenId", token.getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok(TokenResponse.from(token));
    }

    /**
     * 대기열 상태 확인 (Cookie에서 tokenId 추출)
     */
    @GetMapping("/queue/status")
    public ResponseEntity<Integer> getQueueStatus(
            @RequestParam Long scheduleId,
            @CookieValue(value = "tokenId", required = false) String tokenId) {
        return ResponseEntity.ok(reservationApp.getRemaining(scheduleId, tokenId));
    }
}