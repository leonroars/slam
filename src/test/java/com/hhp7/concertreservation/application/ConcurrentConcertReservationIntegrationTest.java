package com.hhp7.concertreservation.application;

import com.hhp7.concertreservation.application.facade.ConcertReservationApplication;
import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.model.SeatStatus;
import com.hhp7.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.hhp7.concertreservation.domain.concert.repository.SeatRepository;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.model.ReservationStatus;
import com.hhp7.concertreservation.domain.reservation.repository.ReservationRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
public class ConcurrentConcertReservationIntegrationTest {

    @Autowired
    private ConcertReservationApplication concertReservationApplication;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static String concertScheduleId;
    private static final String USER_ID_PREFIX = "user-";
    private static final String SEAT_ID_PREFIX  = "seat-";
    //
    // @BeforeAll
    // static void globalSetup() {
    //     // no-op
    // }

    @Test
    @DisplayName("사전준비 : 공연 일정 및 좌석 데이터 생성")
    void setupConcertAndSeats() {
        // given
        ConcertSchedule schedule = ConcertSchedule.create(
                "concert-1",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                50 // available seats
        );
        concertScheduleRepository.save(schedule);
        concertScheduleId = schedule.getId();

        // 서로 다른 seat 50개 생성
        for(int i=1;i<=50;i++){
            Seat seat = Seat.create(SEAT_ID_PREFIX + i, schedule.getId(), 1, 10000, SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // then
        assertThat(concertScheduleRepository.findById(concertScheduleId)).isPresent();
        assertThat(seatRepository.findAllByConcertScheduleId(concertScheduleId)).hasSize(50);
    }

    @Test
    @DisplayName("성공 : 서로 다른 50개 좌석에 대한 동시 예약 시도 -> 모두 성공")
    void concurrentReservationsForDifferentSeats() throws InterruptedException, ExecutionException {

        // given : 50개의 일정한 규칙을 갖는 좌석 생성 및 이에 대한 예약 요청을 수행할 스레드 초기화.
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        // 50명 사용자, 50좌석 동시 예약
        for(int i = 1; i <= 50; i++){
            final int idx = i; // final 변수를 통해 스레드 간 메모리 가시성 확보.
            tasks.add(() -> {
                String userId = USER_ID_PREFIX + idx;
                String seatId = SEAT_ID_PREFIX  + idx;
                // 예약
                Reservation reservation = concertReservationApplication.reserve(concertScheduleId, userId, seatId);

                return reservation != null && reservation.getStatus() == ReservationStatus.PAID;
            });
        }
        Collections.shuffle(tasks);

        // when
        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // then : : 스레드 별 실행 결과를 Future<> 를 통해 확인. 실패하거나 예외가 발생하더라도 카운트가 가능하다.
        int successCount = 0;
        for(Future<Boolean> f : futures){
            if(f.get()) successCount++;
        }
        assertThat(successCount).isEqualTo(50);
    }

    @Test
    @DisplayName("실패 : 동일한 좌석에 대한 동시 5건 예약 시도 -> 1명만 성공, 4명 실패")
    void concurrentReservationsForSameSeat() throws InterruptedException {
        // given
        // seatId 하나를 새로 생성
        Seat seat = Seat.create("commonSeat", concertScheduleId, 1, 10000, SeatStatus.AVAILABLE);
        seatRepository.save(seat);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for(int i=1;i<=5;i++){
            final int idx = i;
            tasks.add(() -> {
                String userId = USER_ID_PREFIX + "same-" + idx;
                try {
                    // 예약
                    Reservation reservation = concertReservationApplication.reserve(concertScheduleId, userId, seat.getId());
                    // 각 스레드 별로 할당된 task(각각 다른 좌석에 대한 예약 수행) 결과가 !null && PAID 인 경우 성공으로 간주 -> true.
                    return reservation != null && "PAID".equals(reservation.getStatus());
                } catch(UnavailableRequestException e){
                    return false; // 실패 처리
                }
            });
        }

        // when : 스레드 풀 내에 존재하는 전체 스레드 작업 모두 시작.
        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // then : SuccessCount == 1 인지 검증한다.
        int successCount = 0;
        int failCount    = 0;
        for(Future<Boolean> f: futures){
            try {
                if(f.get()) successCount++;
            } catch (ExecutionException e){
                failCount++;
            }
        }
        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(4);
    }

}