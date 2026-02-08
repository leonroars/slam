package com.slam.concertreservation.interfaces;

import com.slam.concertreservation.application.facade.ConcertReservationApplication;
import com.slam.concertreservation.application.facade.UserApplication;
import com.slam.concertreservation.component.idempotency.Idempotent;
import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.ConcertScheduleWithConcert;
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

    /* ========== User ========== */

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestParam String name) {
        User user = userApp.registerUser(name);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(UserResponse.from(userApp.getUser(Long.valueOf(userId))));
    }

    /* ========== Point ========== */

    @GetMapping("/users/{userId}/point")
    public ResponseEntity<UserPointBalanceResponse> getUserPointBalance(@PathVariable String userId) {
        UserPointBalance balance = reservationApp.getUserPointBalance(Long.valueOf(userId));
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    @PostMapping("/users/{userId}/point/charge")
    public ResponseEntity<UserPointBalanceResponse> chargeUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        UserPointBalance balance = reservationApp.chargeUserPoint(Long.valueOf(userId), amount);
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    @PostMapping("/users/{userId}/point/use")
    public ResponseEntity<UserPointBalanceResponse> useUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        UserPointBalance balance = reservationApp.useUserPoint(Long.valueOf(userId), amount);
        return ResponseEntity.ok(UserPointBalanceResponse.from(balance));
    }

    /* ========== Concert ========== */

    /**
     * 공연 등록
     */
    @PostMapping("/concerts")
    public ResponseEntity<ConcertResponse> registerConcert(
            @RequestParam String name,
            @RequestParam String artist) {
        Concert concert = reservationApp.registerConcert(name, artist);
        return ResponseEntity.ok(ConcertResponse.from(concert));
    }

    /**
     * 공연 조회
     */
    @GetMapping("/concerts/{concertId}")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        Concert concert = reservationApp.getConcert(concertId);
        return ResponseEntity.ok(ConcertResponse.from(concert));
    }

    /**
     * 공연 일정 등록
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
     * 예약 가능한 공연 일정 목록 조회
     */
    @GetMapping("/concerts/schedules/available")
    public ResponseEntity<List<ConcertScheduleResponse>> getAvailableConcertSchedules() {
        List<ConcertScheduleWithConcert> schedules = reservationApp.getAvailableConcertSchedulesWithConcert();

        return ResponseEntity.ok(schedules.stream()
                .map(r -> ConcertScheduleResponse.from(r.concertSchedule(), r.concert()))
                .toList());
    }

    /**
     * 특정 공연 일정의 예약 가능 좌석 조회
     */
    @GetMapping("/concerts/schedules/{scheduleId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long scheduleId) {
        List<Seat> seats = reservationApp.getAvailableSeats(scheduleId);
        return ResponseEntity.ok(seats.stream().map(SeatResponse::from).toList());
    }

    /* ========== Reservation ========== */

    /**
     * 좌석 선점
     */
    @PostMapping("/concerts/schedules/{scheduleId}/seats/{seatId}/assign")
    public ResponseEntity<SeatResponse> reserveSeat(@PathVariable Long scheduleId,
            @PathVariable Long seatId,
            @RequestParam Long userId) {
        Seat seat = reservationApp.assignSeat(scheduleId, userId, seatId);
        return ResponseEntity.ok(SeatResponse.from(seat));
    }

    /**
     * 가예약 생성
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
     * 예약 확정 (결제 처리)
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
     * 예약 취소
     */
    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationApp.cancelReservation(Long.valueOf(reservationId));
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity.ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * 예약 조회
     */
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationApp.getReservation(Long.valueOf(reservationId));
        Seat seat = reservationApp.getSeat(reservation.getSeatId());
        return ResponseEntity.ok(ReservationResponse.from(reservation, seat));
    }

    /**
     * 사용자의 모든 예약 조회
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
     * 대기열 토큰 발급 후 Cookie(httpOnly)에 저장
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