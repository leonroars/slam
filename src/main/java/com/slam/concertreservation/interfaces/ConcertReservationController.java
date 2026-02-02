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
    public ResponseEntity<User> createUser(@RequestParam String name) {
        User user = userApp.registerUser(name);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(userApp.getUser(Long.valueOf(userId)));
    }

    /* ========== Point ========== */

    @GetMapping("/users/{userId}/point")
    public ResponseEntity<UserPointBalance> getUserPointBalance(@PathVariable String userId) {
        return ResponseEntity.ok(reservationApp.getUserPointBalance(Long.valueOf(userId)));
    }

    @PostMapping("/users/{userId}/point/charge")
    public ResponseEntity<UserPointBalance> chargeUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        return ResponseEntity.ok(reservationApp.chargeUserPoint(Long.valueOf(userId), amount));
    }

    @PostMapping("/users/{userId}/point/use")
    public ResponseEntity<UserPointBalance> useUserPoint(@PathVariable String userId,
            @RequestParam int amount) {
        return ResponseEntity.ok(reservationApp.useUserPoint(Long.valueOf(userId), amount));
    }

    /* ========== Concert ========== */

    /**
     * 공연 등록
     */
    @PostMapping("/concerts")
    public ResponseEntity<Concert> registerConcert(
            @RequestParam String name,
            @RequestParam String artist) {
        Concert concert = reservationApp.registerConcert(name, artist);
        return ResponseEntity.ok(concert);
    }

    /**
     * 공연 조회
     */
    @GetMapping("/concerts/{concertId}")
    public ResponseEntity<Concert> getConcert(@PathVariable String concertId) {
        Concert concert = reservationApp.getConcert(concertId);
        return ResponseEntity.ok(concert);
    }

    /**
     * 공연 일정 등록
     */
    @PostMapping("/concerts/{concertId}/schedules")
    public ResponseEntity<ConcertSchedule> registerConcertSchedule(
            @PathVariable String concertId,
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
        return ResponseEntity.ok(schedule);
    }

    /**
     * 예약 가능한 공연 일정 목록 조회
     */
    @GetMapping("/concerts/schedules/available")
    public ResponseEntity<List<ConcertSchedule>> getAvailableConcertSchedules() {
        return ResponseEntity.ok(reservationApp.getAvailableConcertSchedules());
    }

    /**
     * 특정 공연 일정의 예약 가능 좌석 조회
     */
    @GetMapping("/concerts/schedules/{scheduleId}/seats")
    public ResponseEntity<List<Seat>> getAvailableSeats(@PathVariable String scheduleId) {
        return ResponseEntity.ok(reservationApp.getAvailableSeats(scheduleId));
    }

    /* ========== Reservation ========== */

    /**
     * 좌석 선점
     */
    @PostMapping("/concerts/schedules/{scheduleId}/seats/{seatId}/assign")
    public ResponseEntity<Seat> reserveSeat(@PathVariable String scheduleId,
            @PathVariable String seatId,
            @RequestParam String userId) {
        return ResponseEntity.ok(reservationApp.assignSeat(scheduleId, Long.valueOf(userId), seatId));
    }

    /**
     * 가예약 생성
     */
    @PostMapping("/reservations")
    @Idempotent(operationKey = "reservation.create")
    public ResponseEntity<Reservation> createTemporaryReservation(
            @RequestParam String userId,
            @RequestParam String scheduleId,
            @RequestParam String seatId,
            @RequestParam Integer price) {
        return ResponseEntity
                .ok(reservationApp.createTemporaryReservation(Long.valueOf(userId), scheduleId, seatId, price));
    }

    /**
     * 예약 확정 (결제 처리)
     */
    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<Reservation> confirmReservation(
            @PathVariable String reservationId,
            @RequestParam String userId,
            @RequestParam Integer price) {
        return ResponseEntity
                .ok(reservationApp.paymentRequestForReservation(Long.valueOf(userId), price,
                        Long.valueOf(reservationId)));
    }

    /**
     * 예약 취소
     */
    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable String reservationId) {
        return ResponseEntity.ok(reservationApp.cancelReservation(Long.valueOf(reservationId)));
    }

    /**
     * 예약 조회
     */
    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<Reservation> getReservation(@PathVariable String reservationId) {
        return ResponseEntity.ok(reservationApp.getReservation(Long.valueOf(reservationId)));
    }

    /**
     * 사용자의 모든 예약 조회
     */
    @GetMapping("/users/{userId}/reservations")
    public ResponseEntity<List<Reservation>> getUserReservations(@PathVariable String userId) {
        return ResponseEntity.ok(reservationApp.getUserReservations(Long.valueOf(userId)));
    }

    /* ========== Queue ========== */

    /**
     * 대기열 토큰 발급 후 Cookie(httpOnly)에 저장
     */
    @PostMapping("/queue/tokens")
    public ResponseEntity<Token> issueToken(@RequestParam String userId,
            @RequestParam String scheduleId,
            HttpServletResponse response) {
        Token token = reservationApp.issueToken(Long.valueOf(userId), scheduleId);
        Cookie cookie = new Cookie("tokenId", token.getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok(token);
    }

    /**
     * 대기열 상태 확인 (Cookie에서 tokenId 추출)
     */
    @GetMapping("/queue/status")
    public ResponseEntity<Integer> getQueueStatus(
            @RequestParam String scheduleId,
            @CookieValue(value = "tokenId", required = false) String tokenId) {
        return ResponseEntity.ok(reservationApp.getRemaining(scheduleId, tokenId));
    }
}