package com.slam.concertreservation.application;

import com.slam.concertreservation.application.event.listener.PaymentEventListener;
import com.slam.concertreservation.application.facade.ConcertReservationApplication;
import com.slam.concertreservation.application.facade.UserApplication;
import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.ConcertScheduleWithConcert;
import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.concert.model.SeatStatus;
import com.slam.concertreservation.domain.concert.repository.ConcertRepository;
import com.slam.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.slam.concertreservation.domain.concert.repository.SeatRepository;
import com.slam.concertreservation.domain.point.model.PointHistory;
import com.slam.concertreservation.domain.point.model.PointTransactionType;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.point.repository.PointHistoryRepository;
import com.slam.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.slam.concertreservation.domain.point.service.PointService;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.model.ReservationStatus;
import com.slam.concertreservation.domain.reservation.repository.ReservationRepository;
import com.slam.concertreservation.domain.reservation.service.ReservationService;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class NonConcurrentConcertReservationIntegrationTest {

        private static final Logger log = LoggerFactory.getLogger(NonConcurrentConcertReservationIntegrationTest.class);

        @Autowired
        private ConcertReservationApplication concertReservationApplication;

        @Autowired
        private PaymentEventListener paymentEventListener;

        @Autowired
        private UserApplication userApplication;

        @Autowired
        private ConcertRepository concertRepository;

        @Autowired
        private ReservationRepository reservationRepository;

        @Autowired
        private ConcertScheduleRepository concertScheduleRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TokenRepository tokenRepository;

        @Autowired
        private SeatRepository seatRepository;

        @Autowired
        private PointHistoryRepository pointHistoryRepository;

        @Autowired
        private UserPointBalanceRepository userPointBalanceRepository;

        String userName = "userName";
        String concertName = "concertName";
        String artistName = "oasis";
        LocalDateTime concertDateTime = LocalDateTime.now().plusDays(15); // Concert in 15 days
        LocalDateTime reservationStartAt = LocalDateTime.now(); // Reservation starts now
        LocalDateTime reservationEndAt = LocalDateTime.now().plusDays(14); // Reservation ends 1 day before concert
        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, concertDateTime, reservationStartAt,
                        reservationEndAt);

        @Autowired
        private PointService pointService;
        @Autowired
        private ReservationService reservationService;

        @Nested
        class UserIntegrationTest {
                @Test
                @DisplayName("성공 : 회원 가입 시 회원 조회가 가능하다.")
                void shouldFindUser_WhenUserSignedUp() {
                        // given
                        String userName = "한성경";

                        // when
                        User createdUser = userApplication.registerUser(userName);
                        User foundUser = userApplication.getUser(createdUser.getId());

                        // then
                        Assertions.assertEquals(createdUser.getId(), foundUser.getId());
                }
        }

        @Nested
        class UserPointIntegrationTest {

                @Test
                @DisplayName("성공 : 회원 가입 시 해당 회원의 포인트 잔액 0인 UserPointBalance가 생성되어 저장된다. 이때 INIT 타입의 PointHistory도 생성되어 저장된다.")
                void shouldCreateAndSaveUserPointBalanceWith0AndInitPointHistory_WhenUserSignedUp() {
                        // given
                        String userName = "한성경";

                        // when
                        User createdUser = userApplication.registerUser(userName);
                        UserPointBalance actualUserPointBalance = concertReservationApplication
                                        .getUserPointBalance(createdUser.getId());
                        List<PointHistory> actualPointHistory = concertReservationApplication
                                        .getPointHistories(createdUser.getId());

                        // then
                        Assertions.assertEquals(0, actualUserPointBalance.balance().getAmount());
                        Assertions.assertEquals(1, actualPointHistory.size());
                        Assertions.assertEquals(0, actualPointHistory.get(0).transactionAmount());
                        Assertions.assertEquals(PointTransactionType.INIT, actualPointHistory.get(0).transactionType());
                }

                @Test
                @DisplayName("성공 : 회원의 포인트 잔액을 증액한다. 이때 PointHistory가 생성되어 저장된다.")
                void shouldIncreaseUserPointBalanceAndCreatePointHistoryOfCharge_whenUserChargesPoint() {
                        // given
                        User user = userApplication.registerUser(userName);

                        // when
                        UserPointBalance actualUserPointBalance = concertReservationApplication
                                        .chargeUserPoint(user.getId(), 1000);
                        List<PointHistory> actualPointHistory = concertReservationApplication
                                        .getPointHistories(user.getId());

                        // then
                        Assertions.assertNotNull(actualUserPointBalance);
                        Assertions.assertEquals(1000, actualUserPointBalance.balance().getAmount());

                        Assertions.assertNotNull(actualPointHistory);
                        Assertions.assertEquals(2, actualPointHistory.size());
                }

                @Test
                @DisplayName("성공 : 회원의 포인트 잔액을 감소한다. 이때 USE 타입의 PointHistory가 생성되어 저장된다.")
                void shouldDecreaseUserPointBalanceAndCreatePointHistoryOfUse_whenUserUsesPoint() {
                        // given
                        User user = userApplication.registerUser("정종환");
                        concertReservationApplication.chargeUserPoint(user.getId(), 1000);

                        // when
                        UserPointBalance actualUserPointBalance = concertReservationApplication
                                        .useUserPoint(user.getId(), 500);
                        PointHistory expectedPointHistory = PointHistory.create(user.getId(), PointTransactionType.USE,
                                        500);
                        List<PointHistory> actualPointHistory = concertReservationApplication
                                        .getPointHistories(user.getId());

                        // then
                        Assertions.assertNotNull(actualUserPointBalance);
                        Assertions.assertEquals(500, actualUserPointBalance.balance().getAmount());

                        Assertions.assertNotNull(actualPointHistory);
                        Assertions.assertEquals(3, actualPointHistory.size());
                }
        }

        @Nested
        class ConcertReservationIntegrationTest {

                @Test
                @DisplayName("성공 : 예약 가능한 공연 일정을 조회한다.")
                void shouldSuccessfullyGetAvailableConcertSchedule() {
                        // given
                        // Create a concert first to ensure it exists for the schedule
                        Concert concert = concertReservationApplication.registerConcert("Test Concert", "Test Artist");

                        // Create an available schedule (start time <= now)
                        ConcertSchedule expected = concertReservationApplication.registerConcertSchedule(
                                        concert.getId(),
                                        concertDateTime.plusDays(1), LocalDateTime.now().minusMinutes(1),
                                        reservationEndAt.plusDays(1),
                                        1000);

                        // when
                        List<ConcertScheduleWithConcert> actual = concertReservationApplication
                                        .getAvailableConcertSchedulesWithConcert();

                        // then
                        Assertions.assertNotNull(actual);
                        // Verify that the schedule we just created is present in the results
                        boolean isPresent = actual.stream()
                                        .anyMatch(cs -> cs.concertSchedule().getId().equals(expected.getId()));
                        Assertions.assertTrue(isPresent, "Expected schedule should be available");
                }

                @Test
                @DisplayName("성공 : 특정 공연 일정의 예약 가능 좌석 목록을 조회한다.")
                void shouldSuccessfullyGetAvailableSeats() {
                        // given
                        ConcertSchedule concertSchedule = concertReservationApplication.registerConcertSchedule(1L,
                                        concertDateTime, reservationStartAt, reservationEndAt, 1000);

                        // when
                        List<Seat> actual = concertReservationApplication.getAvailableSeats(concertSchedule.getId());

                        // then
                        Assertions.assertNotNull(actual);
                        Assertions.assertEquals(Seat.MAX_SEAT_NUMBER, actual.size());
                }

                @Test
                @DisplayName("성공 : 특정 공연 일정의 특정 좌석을 예약하고 결제한다. 예약 후 해당 좌석 조회 시 상태는 UNAVAILABLE 이다. 또한 예약 상태는 PAID 이다. 잔액은 차감된다.")
                void shouldSuccessfullyConfirmAndPaySeat() throws InterruptedException {
                        // given
                        User user = userApplication.registerUser(userName);
                        concertReservationApplication.chargeUserPoint(user.getId(), 2000);

                        ConcertSchedule concertSchedule = concertReservationApplication.registerConcertSchedule(1L,
                                        concertDateTime, reservationStartAt, reservationEndAt, 1000);
                        Seat seat = concertReservationApplication.getAvailableSeats(concertSchedule.getId()).get(0);

                        // when
                        Seat assignedSeat = concertReservationApplication.assignSeat(concertSchedule.getId(),
                                        user.getId(),
                                        seat.getId()); // 좌석 선점
                        Reservation createdTemporaryReservation = concertReservationApplication
                                        .createTemporaryReservation(
                                                        user.getId(), concertSchedule.getId(), assignedSeat.getId(),
                                                        assignedSeat.getPrice()); // 가예약 생성

                        concertReservationApplication.paymentRequestForReservation(user.getId(), seat.getPrice(),
                                        createdTemporaryReservation.getId()); // 결제 요청

                        UserPointBalance updatedUserPointBalance = concertReservationApplication
                                        .getUserPointBalance(user.getId()); // 차감된
                                                                            // 사용자
                                                                            // 잔액
                        Seat reservedSeat = concertReservationApplication.getSeat(seat.getId()); // 예약된 좌석
                        Reservation confirmedReservation = concertReservationApplication
                                        .getReservation(createdTemporaryReservation.getId()); // 확정된 예약
                        log.warn("결제 완료 후 예약 확정되어 저장된 Reservation 상태 : {}", confirmedReservation.getStatus());
                        log.warn("결제 완료 후 예약 확정되어 저장된 Reservation ID : {}", confirmedReservation.getId());

                        Thread.sleep(1000);

                        // then
                        Assertions.assertEquals(updatedUserPointBalance.balance().getAmount(), 1000);
                        Assertions.assertEquals(SeatStatus.UNAVAILABLE, reservedSeat.getStatus());
                        Assertions.assertEquals(ReservationStatus.CONFIRMED, confirmedReservation.getStatus());
                        Assertions.assertEquals(1000, updatedUserPointBalance.balance().getAmount());
                }
        }
}
