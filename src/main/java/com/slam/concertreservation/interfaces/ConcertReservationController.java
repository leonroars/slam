package com.slam.concertreservation.interfaces;

import com.slam.concertreservation.application.facade.ConcertReservationApplication;
import com.slam.concertreservation.application.facade.UserApplication;
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
        return ResponseEntity.ok(userApp.getUser(userId));
    }

    /* ========== Point ========== */

    @GetMapping("/users/{userId}/point")
    public ResponseEntity<UserPointBalance> getUserPointBalance(@PathVariable String userId) {
        return ResponseEntity.ok(reservationApp.getUserPointBalance(userId));
    }

    @PostMapping("/users/{userId}/point/charge")
    public ResponseEntity<UserPointBalance> chargeUserPoint(@PathVariable String userId,
                                                            @RequestParam int amount) {
        return ResponseEntity.ok(reservationApp.chargeUserPoint(userId, amount));
    }

    @PostMapping("/users/{userId}/point/use")
    public ResponseEntity<UserPointBalance> useUserPoint(@PathVariable String userId,
                                                         @RequestParam int amount) {
        return ResponseEntity.ok(reservationApp.useUserPoint(userId, amount));
    }

    /* ========== Concert ========== */

    /**
     * 공연 등록
     */
    @PostMapping
    public ResponseEntity<Concert> registerConcert(
            @RequestParam String name,
            @RequestParam String artist) {
        Concert concert = reservationApp.registerConcert(name, artist);
        return ResponseEntity.ok(concert);
    }

    /**
     * 공연 조회
     */
    @GetMapping("/{concertId}")
    public ResponseEntity<Concert> getConcert(@PathVariable String concertId) {
        Concert concert = reservationApp.getConcert(concertId);
        return ResponseEntity.ok(concert);
    }

    /**
     * 공연 일정 등록
     */
    @PostMapping("/{concertId}/schedules")
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



    @GetMapping("/concertSchedules/available")
    public ResponseEntity<List<ConcertSchedule>> getAvailableConcertSchedules() {
        return ResponseEntity.ok(reservationApp.getAvailableConcertSchedules());
    }

    @GetMapping("/concertSchedules/{scheduleId}/seats")
    public ResponseEntity<List<Seat>> getAvailableSeats(@PathVariable String scheduleId) {
        return ResponseEntity.ok(reservationApp.getAvailableSeats(scheduleId));
    }

    /* ========== Reservation ========== */

    /**
     * 좌석 선점
     */
    @PostMapping("/concertSchedules/{scheduleId}/assign-seat")
    public ResponseEntity<Seat> reserveSeat(@PathVariable String scheduleId,
                                                   @RequestParam String userId,
                                                   @RequestParam String seatId) {
        return ResponseEntity.ok(reservationApp.assignSeat(scheduleId, userId, seatId));
    }

    /**
     * 가예약 생성
     * @param scheduleId
     * @param userId
     * @param seatId
     * @param price
     * @return
     */
    @PostMapping("/concertSchedules/{scheduleId}/temp-reservation")
    public ResponseEntity<Reservation> tempReservation(@PathVariable String scheduleId,
                                                       @RequestParam String userId,
                                                       @RequestParam String seatId,
                                                       @RequestParam Integer price) {
        return ResponseEntity.ok(reservationApp.createTemporaryReservation(userId, scheduleId, seatId, price));
    }

    /**
     * 예약 확정
     */
    @PostMapping("/concertSchedules/{scheduleId}/confirmReservation")
    public ResponseEntity<Reservation> paymentRequest(@RequestParam String userId,
                                                      @RequestParam Integer price,
                                                 @RequestParam String reservationId) {
        return ResponseEntity.ok(reservationApp.paymentRequestForReservation(userId, price, reservationId));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable String reservationId) {
        return ResponseEntity.ok(reservationApp.cancelReservation(reservationId));
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<Reservation> getReservation(@PathVariable String reservationId) {
        return ResponseEntity.ok(reservationApp.getReservation(reservationId));
    }

    /* ========== Queue ========== */
    /**
     * 대기열 토큰 발급 후 Cookie(httpOnly)에 저장
     */
    @PostMapping("/queue/token")
    public ResponseEntity<Token> issueToken(@RequestParam String userId,
                                            @RequestParam String scheduleId,
                                            HttpServletResponse response) {
        Token token = reservationApp.issueToken(userId, scheduleId);
        Cookie cookie = new Cookie("tokenId", token.getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok(token);
    }

    /**
     * 대기열 폴링: Cookie에서 tokenId 추출 후, 대기 상태 확인
     */
    @GetMapping("/queue/remaining")
    public ResponseEntity<Integer> getRemaining(@RequestParam String scheduleId,
                                                @CookieValue(value = "tokenId", required = false) String tokenId) {
        return ResponseEntity.ok(reservationApp.getRemaining(scheduleId, tokenId));
    }
}