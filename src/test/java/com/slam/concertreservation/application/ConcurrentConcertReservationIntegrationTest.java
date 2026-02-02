package com.slam.concertreservation.application;

import com.slam.concertreservation.application.facade.ConcertReservationApplication;
import com.slam.concertreservation.application.facade.UserApplication;
import com.slam.concertreservation.common.exceptions.ConcurrencyException;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.concert.model.SeatStatus;
import com.slam.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.slam.concertreservation.domain.concert.repository.SeatRepository;
import com.slam.concertreservation.domain.concert.service.ConcertService;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.reservation.repository.ReservationRepository;
import com.slam.concertreservation.domain.user.model.User;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConcurrentConcertReservationIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentConcertReservationIntegrationTest.class);
    @Autowired
    private ConcertReservationApplication concertReservationApplication;

    @Autowired
    private UserApplication userApplication;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final int SEAT_PRICE = 1000;
    private static List<User> users = new ArrayList<>(); // 사용자 Pool
    private static List<Seat> seats = new ArrayList<>(); // 좌석 Pool
    private static final LocalDateTime dateTime = LocalDateTime.now().plusDays(3);
    private static final LocalDateTime reservationStartAt = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime reservationEndAt = LocalDateTime.now().plusDays(2);
    @Autowired
    private ConcertService concertService;

    @Nested
    class ConcertReservationTest {

        @Test
        @DisplayName("성공 : 서로 다른 50개 좌석에 대한 동시 좌석 선점(가예약) 시도 -> 모두 성공, 예약 가능 잔여 좌석 0.")
        void concurrentReservationsForDifferentSeats() throws InterruptedException, ExecutionException {
            // given : 50명의 사용자와 50개의 좌석이 존재한다.

            // 1) 사용자 50명 생성 및 포인트 충전
            List<User> localUsers = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                User createdUser = userApplication.registerUser("userName" + i);
                concertReservationApplication.chargeUserPoint(createdUser.getId(), 2000);
                localUsers.add(createdUser);
            }

            // 2) 공연 일정 등록
            long startTime = System.currentTimeMillis();
            ConcertSchedule registeredConcertSchedule = concertReservationApplication.registerConcertSchedule(
                    1L, dateTime, reservationStartAt, reservationEndAt, SEAT_PRICE);
            long endTime = System.currentTimeMillis();

            // 3) 공연 일정의 좌석 목록 조회
            List<Seat> localSeats = concertReservationApplication.getAvailableSeats(registeredConcertSchedule.getId());

            // when : 50명의 사용자가 50개의 좌석에 대한 예약을 동시에 시도.
            ExecutorService executor = Executors.newFixedThreadPool(50);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            // 50명 사용자, 50좌석 동시 예약
            for (int i = 1; i <= 50; i++) {
                final int idx = i; // final 변수를 통해 스레드 간 메모리 가시성 확보.
                tasks.add(() -> {
                    try {
                        // 가예약 생성
                        Seat assignedSeat = concertReservationApplication.assignSeat(
                                registeredConcertSchedule.getId(),
                                localUsers.get(idx - 1).getId(),
                                localSeats.get(idx - 1).getId());
                        // 각 스레드 별 작업 결과가 !null && "UNAVAILABLE"인 경우 성공으로 간주
                        return assignedSeat != null && (assignedSeat.getStatus() == SeatStatus.UNAVAILABLE);
                    } catch (UnavailableRequestException e) {
                        return false; // 실패 처리
                    }
                });
            }
            Collections.shuffle(tasks);

            // 50개의 요청 동시 실행. 처리된 결과는 Future<> 인터페이스 활용해 비동기적으로 확인.
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            AtomicInteger successCount = new AtomicInteger(0);
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    successCount.incrementAndGet();
                }
            }

            ConcertSchedule updatedConcertSchedule = concertReservationApplication
                    .getConcertSchedule(registeredConcertSchedule.getId());

            // then : 스레드 별 실행 결과를 Future<> 를 통해 확인
            assertThat(successCount.get()).isEqualTo(50); // 모든 예약이 성공했는지 확인
            // assertThat(updatedConcertSchedule.getAvailability()).isEqualTo(ConcertScheduleAvailability.SOLDOUT);
            // // 매진 상태인지 확인.
            assertThat(concertService.getRemainingSeatsCount(updatedConcertSchedule.getId())).isEqualTo(0); // 예약 가능 좌석이
                                                                                                            // 0인지 확인.
        }

        @Test
        @DisplayName("실패 : 동일한 좌석에 대한 동시 5건 예약 시도 -> 1명만 성공, 4명 실패 && 예약 가능 좌석 49석.")
        void concurrentReservationsForSameSeat() throws InterruptedException, ExecutionException {
            // ============= [Setup 로직] =============
            // 1) 사용자 5명 생성 및 포인트 충전
            List<User> localUsers = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                User createdUser = userApplication.registerUser("userName" + i);
                concertReservationApplication.chargeUserPoint(createdUser.getId(), 2000);
                localUsers.add(createdUser);
            }

            // 2) 공연 일정 등록
            ConcertSchedule registeredConcertSchedule = concertReservationApplication.registerConcertSchedule(
                    1L, dateTime, reservationStartAt, reservationEndAt, SEAT_PRICE);

            // 3) 공연 일정의 좌석 목록 조회
            List<Seat> localSeats = concertReservationApplication.getAvailableSeats(registeredConcertSchedule.getId());
            // given : 5명의 사용자가 동일한 좌석에 대한 예약을 동시에 시도.
            ExecutorService executor = Executors.newFixedThreadPool(5);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            // 5명 사용자, 동일 좌석 동시 예약
            for (int i = 1; i <= 5; i++) {
                int idx = i;
                tasks.add(() -> {
                    try {
                        // 가예약 생성
                        Seat assignedSeat = concertReservationApplication.assignSeat(
                                registeredConcertSchedule.getId(),
                                localUsers.get(idx - 1).getId(),
                                localSeats.get(0).getId());
                        // 각 스레드 별로 할당된 task(각각 동일 좌석에 대한 예약 수행) 결과가 !null && BOOKED 인 경우 성공으로 간주 ->
                        // true.
                        return assignedSeat != null && (assignedSeat.getStatus() == SeatStatus.UNAVAILABLE);
                    } catch (UnavailableRequestException | BusinessRuleViolationException | ConcurrencyException e) {
                        return false; // 실패 처리
                    }
                });
            }

            // when : 5명의 사용자가 동일한 좌석에 대한 예약을 동시에 시도.
            Collections.shuffle(tasks);
            List<Future<Boolean>> futures = executor.invokeAll(tasks);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            }

            ConcertSchedule updatedConcertSchedule = concertReservationApplication
                    .getConcertSchedule(registeredConcertSchedule.getId());

            // then
            assertThat(successCount.get()).isEqualTo(1); // 1명만 성공해야 함.
        }
    }

    @Nested
    class ConcurrentUserPointTest {
        @Test
        @DisplayName("성공 : 동시 사용 요청 2건 중 한 건만 성공한다.")
        void onlyOneUsageRequestShouldSuccess_WhenThereAreNUsageRequestForSingleUser()
                throws InterruptedException, ExecutionException {

            // given : 잔액이 0인 사용자가 존재한다.
            User user = userApplication.registerUser("userName");

            // when : 해당 사용자에 대한 2건의 충전 요청이 동시에 발생한다.
            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Callable<Boolean>> tasks = new ArrayList<>();
            tasks.add(() -> {
                try {
                    concertReservationApplication.chargeUserPoint(user.getId(), 1000);
                    return true;
                } catch (UnavailableRequestException e) {
                    return false;
                }
            });

            Collections.shuffle(tasks);
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            AtomicInteger successCount = new AtomicInteger(0);

            for (Future<Boolean> f : futures) {
                if (f.get())
                    successCount.incrementAndGet();
            }

            UserPointBalance updatedUserPointBalance = concertReservationApplication.getUserPointBalance(user.getId());

            // then : 1건만 성공하고 1건은 실패한다. 이에 따라 해당 사용자의 잔액은 1,000이다.
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(updatedUserPointBalance.balance().getAmount()).isEqualTo(1000);

        }

        @Test
        @DisplayName("성공 : 동시 충전 요청 2건 중 한 건만 성공한다.")
        void onlyOneChargeRequestShouldSuccess_WhenThereAreNChargeRequestForSingleUser()
                throws ExecutionException, InterruptedException {
            // given : 잔액이 0인 사용자가 존재한다.
            User user = userApplication.registerUser("userName");

            // when : 해당 사용자에 대한 2건의 충전 요청이 동시에 발생한다.
            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tasks.add(() -> {
                    try {
                        concertReservationApplication.chargeUserPoint(user.getId(), 1000);
                        return true;
                    } catch (UnavailableRequestException | ObjectOptimisticLockingFailureException e) {
                        return false;
                    }
                });
            }

            Collections.shuffle(tasks);
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            AtomicInteger successCount = new AtomicInteger(0);

            for (Future<Boolean> f : futures) {
                if (f.get())
                    successCount.incrementAndGet();
            }

            UserPointBalance updatedUserPointBalance = concertReservationApplication.getUserPointBalance(user.getId());

            // then : 1건만 성공하고 1건은 실패한다. 이에 따라 해당 사용자의 잔액은 1,000이다.
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(updatedUserPointBalance.balance().getAmount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("성공 : 동시 다발적 복수 사용자의 포인트 충전 요청은 모두 성공한다.")
        void allChargeRequestShouldSuccess_WhenThereAreMultipleUserChargeRequest() {
            // given : 10명의 사용자가 존재한다. 각 사용자의 잔액은 모두 0이다.
            List<User> localUsers = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                User createdUser = userApplication.registerUser("userName" + i);
                localUsers.add(createdUser);
            }

            // when : 10명의 사용자에 대한 동시 충전 요청이 발생한다.
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                tasks.add(() -> {
                    try {
                        concertReservationApplication.chargeUserPoint(localUsers.get(idx).getId(), 1000);
                        return true;
                    } catch (UnavailableRequestException e) {
                        return false;
                    }
                });
            }

            // then : 모든 사용자의 충전 요청이 성공한다.
            AtomicInteger successCount = new AtomicInteger(0);
            for (Callable<Boolean> task : tasks) {
                try {
                    if (task.call())
                        successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Error occurred while executing task.", e);
                }
            }

            assertThat(successCount.get()).isEqualTo(10);
        }
    }

}