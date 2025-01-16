package com.hhp7.concertreservation.application;

import static org.mockito.Mockito.verify;

import com.hhp7.concertreservation.application.facade.ConcertReservationApplication;
import com.hhp7.concertreservation.application.facade.UserApplication;
import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.model.SeatStatus;
import com.hhp7.concertreservation.domain.concert.repository.ConcertRepository;
import com.hhp7.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.hhp7.concertreservation.domain.concert.repository.SeatRepository;
import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import com.hhp7.concertreservation.domain.point.repository.PointHistoryRepository;
import com.hhp7.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.hhp7.concertreservation.domain.queue.repository.TokenRepository;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.model.ReservationStatus;
import com.hhp7.concertreservation.domain.reservation.repository.ReservationRepository;
import com.hhp7.concertreservation.domain.user.model.User;
import com.hhp7.concertreservation.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class NonConcurrentConcertReservationIntegrationTest {
    @Autowired
    private ConcertReservationApplication concertReservationApplication;

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
    LocalDateTime concertDateTime = LocalDateTime.of(2025, 12, 31, 23, 50, 50);
    LocalDateTime reservationStartAt = LocalDateTime.now();
    LocalDateTime reservationEndAt = LocalDateTime.of(2025, 11, 30, 23, 59, 59);
    ConcertSchedule concertSchedule = ConcertSchedule.create("1", concertDateTime, reservationStartAt, reservationEndAt);

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
    class ConcertReservationIntegrationTest {
        @Test
        @DisplayName("성공 : 예약 가능한 공연 일정을 조회한다.")
        void shouldSuccessfullyGetAvailableConcertSchedule() {
            // given
            ConcertSchedule expected = concertReservationApplication.registerConcertSchedule("1", concertDateTime, reservationStartAt, reservationEndAt, 1000);

            // when
            List<ConcertSchedule> actual = concertReservationApplication.getAvailableConcertSchedules();

            // then
            Assertions.assertNotNull(actual);
            Assertions.assertEquals(1, actual.size());
            Assertions.assertEquals(expected.getId(), actual.get(0).getId());

        }

        @Test
        @DisplayName("성공 : 특정 공연 일정의 예약 가능 좌석 목록을 조회한다.")
        void shouldSuccessfullyGetAvailableSeats() {
            // given
            ConcertSchedule concertSchedule = concertReservationApplication.registerConcertSchedule("1", concertDateTime, reservationStartAt, reservationEndAt, 1000);

            // when
            List<Seat> actual = concertReservationApplication.getAvailableSeats(concertSchedule.getId());

            // then
            Assertions.assertNotNull(actual);
            Assertions.assertEquals(Seat.MAX_SEAT_NUMBER, actual.size());
        }

        @Test
        @DisplayName("성공 : 특정 공연 일정의 특정 좌석을 예약하고 결제한다. 예약 후 해당 좌석 조회 시 상태는 UNAVAILABLE 이다. 또한 예약 상태는 PAID 이다. 잔액은 차감된다.")
        void shouldSuccessfullyReserveAndPaySeat() {
            // given
            User user = userApplication.registerUser(userName);

            UserPointBalance userPointBalance = concertReservationApplication.chargeUserPoint(user.getId(), 2000);
            ConcertSchedule concertSchedule = concertReservationApplication.registerConcertSchedule("1", concertDateTime, reservationStartAt, reservationEndAt, 1000);
            Seat seat = concertReservationApplication.getAvailableSeats(concertSchedule.getId()).get(0);

            // when
            Reservation reservation = concertReservationApplication.reserve(concertSchedule.getId(), user.getId(), seat.getId());
            Seat reservedSeat = concertReservationApplication.getSeat(seat.getId());

            // then
            Assertions.assertEquals(SeatStatus.UNAVAILABLE, reservedSeat.getStatus());
            Assertions.assertEquals(ReservationStatus.PAID, reservation.getStatus());
            Assertions.assertEquals(1000, userPointBalance.balance().getAmount());
        }
    }
}
