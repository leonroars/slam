package com.slam.concertreservation.domain.concert.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.ConcertScheduleWithConcert;
import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.concert.model.SeatStatus;
import com.slam.concertreservation.domain.concert.repository.ConcertRepository;
import com.slam.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.slam.concertreservation.domain.concert.repository.SeatRepository;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.List;
import java.util.Optional;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConcertServiceUnitTest {

        @Mock
        private ConcertRepository concertRepository;

        @Mock
        private ConcertScheduleRepository concertScheduleRepository;

        @Mock
        private SeatRepository seatRepository;

        @Mock
        private ApplicationEventPublisher applicationEventPublisher;

        @InjectMocks
        private ConcertService concertService;

        LocalDateTime first = LocalDateTime.of(2022, 1, 1, 0, 0);
        LocalDateTime second = LocalDateTime.of(2022, 1, 2, 0, 0);
        LocalDateTime third = LocalDateTime.of(2022, 1, 3, 0, 0);
        int price = 1000;

        @BeforeEach
        void setup() {
                MockitoAnnotations.openMocks(this);
        }

        @Nested
        class RegisterConcertUnitTest {
                @Test
                @DisplayName("성공 : Concert를 등록하면 저장된 Concert를 반환한다.")
                void shouldReturnSavedConcert_WhenRegisterConcert() {
                        // given
                        Concert expected = Concert.create("OASIS REUNION", "oasis");

                        when(concertRepository.save(any(Concert.class)))
                                        .thenReturn(expected);

                        // when
                        Concert actual = concertService.registerConcert(expected);

                        // then
                        assertEquals(expected.getId(), actual.getId());
                }
        }

        @Nested
        class GetConcertUnitTest {
                @Test
                @DisplayName("성공 : 존재하는 Concert ID로 조회하면 해당 Concert를 반환한다.")
                void shouldReturnConcert_WhenGetExistingConcert() {
                        // given
                        Concert expected = Concert.create("OASIS REUNION", "oasis");

                        when(concertRepository.save(any(Concert.class)))
                                        .thenReturn(expected);
                        when(concertRepository.findById(expected.getId()))
                                        .thenReturn(Optional.of(expected));

                        // when
                        Concert actual = concertService.getConcert(expected.getId());

                        // then
                        assertEquals(expected, actual);
                }

                @Test
                @DisplayName("실패 : 존재하지 않는 Concert ID로 조회하려 하면 UnavailableRequestException이 발생하며 실패한다.")
                void shouldThrowUnavailableRequestException_WhenConcertNotFound() {
                        // given
                        Concert expected = Concert.create("OASIS REUNION", "oasis");

                        when(concertRepository.save(any(Concert.class)))
                                        .thenReturn(expected);
                        when(concertRepository.findById(expected.getId()))
                                        .thenReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.getConcert(expected.getId()))
                                        .isInstanceOf(UnavailableRequestException.class);
                }
        }

        @Nested
        class RegisterConcertScheduleUnitTest {

                @Test
                @DisplayName("성공 : 정상적인 ConcertSchedule을 등록하면 해당 ConcertSchedule이 생성되고 조회가 가능하다.")
                void shouldRegisterConcertSchedule_WhenValidConcertScheduleHasGiven() {
                        // given
                        ConcertSchedule expected = ConcertSchedule.create(1L, third, first, second);

                        when(concertScheduleRepository.save(any(ConcertSchedule.class)))
                                        .thenReturn(expected);

                        // when
                        ConcertSchedule actual = concertService.registerConcertSchedule(expected, price);

                        // then
                        verify(concertScheduleRepository, times(1)).save(expected);
                        assertEquals(expected.getConcertId(), actual.getConcertId());
                }

                @Test
                @DisplayName("실패 : ConcertSchedule 저장 시 예외가 발생하면 좌석이 생성되지 않고 실패한다.")
                void shouldFailToRegisterConcertSchedule_WhenConcertScheduleSaveFails() {
                        // given
                        ConcertSchedule expected = ConcertSchedule.create(1L, third, first, second);

                        when(concertScheduleRepository.save(any(ConcertSchedule.class)))
                                        .thenThrow(new BusinessRuleViolationException(ErrorCode.INTERNAL_SERVER_ERROR,
                                                        "ConcertSchedule 저장 실패"));

                        // when & then

                        // 예외 발생 여부 체크
                        assertThatThrownBy(
                                        () -> concertService.registerConcertSchedule(expected, price))
                                        .isInstanceOf(BusinessRuleViolationException.class);

                        // 저장 메서드 자체는 호출되었으나 예외 발생.
                        verify(concertScheduleRepository, times(1)).save(expected);

                        // 원자적으로 묶여있는 SeatRepository 에 대한 saveAll 메서드는 호출되지 않음을 검증.
                        verifyNoInteractions(seatRepository);
                }
        }

        @Nested
        class AssignSeatOfConcertScheduleUnitTest {
                @Test
                @DisplayName("성공 : 특정 ConcertSchedule의 특정 좌석을 배정하면 좌석 상태가 변경된다.")
                void shouldAssignSeatAndDecrementAvailableSeats_WhenValidSeatAndSchedule() {
                        // given
                        Long userId = 1L;
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, 1L, third, first, second);
                        Seat seat = Seat.create(1L, 1, 1000, SeatStatus.AVAILABLE);

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seat.getId()))
                                        .thenReturn(Optional.of(seat));
                        when(seatRepository.save(any(Seat.class)))
                                        .thenReturn(seat);
                        when(concertScheduleRepository.save(any(ConcertSchedule.class)))
                                        .thenReturn(concertSchedule);

                        // when
                        Seat assignedSeat = concertService.assignSeatOfConcertSchedule(concertSchedule.getId(),
                                        seat.getId(), userId);

                        // then
                        assertEquals(SeatStatus.UNAVAILABLE, assignedSeat.getStatus());
                }

                @Test
                @DisplayName("실패 : 존재하지 않는 Seat를 배정하려 하면 UnavailableRequestException이 발생하며 실패한다.")
                void shouldThrowUnavailableRequestException_WhenSeatNotFoundDuringAssignment() {
                        // given
                        Long userId = 1L;
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, 1L, third, first, second);
                        Long seatId = 1L;

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seatId))
                                        .thenReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.assignSeatOfConcertSchedule(concertSchedule.getId(),
                                                        seatId, userId))
                                        .isInstanceOf(UnavailableRequestException.class);

                }

                @Test
                @DisplayName("실패 : 존재하지 않는 ConcertSchedule에 좌석을 배정하려 하면 UnavailableRequestException이 발생하며 실패한다.")
                void shouldThrowUnavailableRequestException_WhenConcertScheduleNotFoundDuringAssignment() {
                        // given
                        Long concertScheduleId = 1L;
                        Long seatId = 1L;
                        Long userId = 1L;

                        when(concertScheduleRepository.findById(concertScheduleId))
                                        .thenReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.assignSeatOfConcertSchedule(concertScheduleId, seatId,
                                                        userId))
                                        .isInstanceOf(UnavailableRequestException.class);
                }

                @Test
                @DisplayName("실패 : 이미 배정된 좌석을 다시 배정하려 하면 BusinessRuleViolation 발생하며 실패한다.")
                void shouldThrowIllegalStateException_WhenSeatAlreadyAssigned() {
                        // given
                        Long userId = 1L;
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, 1L, third, first, second);
                        Seat seat = Seat.create(1L, 1, 1000, SeatStatus.UNAVAILABLE);

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seat.getId()))
                                        .thenReturn(Optional.of(seat));

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.assignSeatOfConcertSchedule(concertSchedule.getId(),
                                                        seat.getId(), userId))
                                        .isInstanceOf(BusinessRuleViolationException.class);
                }
        }

        @Nested
        class UnassignSeatOfConcertScheduleUnitTest {
                @Test
                @DisplayName("성공 : 특정 ConcertSchedule의 특정 좌석을 배정 해제하면 좌석 상태가 변경된다.")
                void shouldUnassignSeatAndIncrementAvailableSeats_WhenValidSeatAndSchedule() {
                        // given
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, third, first, second);
                        Seat seat = Seat.create(1L, 1, 1000, SeatStatus.UNAVAILABLE);

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seat.getId()))
                                        .thenReturn(Optional.of(seat));
                        when(seatRepository.save(any(Seat.class)))
                                        .thenReturn(seat);
                        when(concertScheduleRepository.save(any(ConcertSchedule.class)))
                                        .thenReturn(concertSchedule);

                        // when
                        Seat unassignedSeat = concertService.unassignSeatOfConcertSchedule(concertSchedule.getId(),
                                        seat.getId());

                        // then
                        assertEquals(SeatStatus.AVAILABLE, unassignedSeat.getStatus());
                }

                @Test
                @DisplayName("실패 : 존재하지 않는 Seat를 배정 해제하려 하면 UnavailableRequestException 이 발생하며 실패한다.")
                void shouldThrowUnavailableRequestException_WhenSeatNotFoundDuringUnassignment() {
                        // given
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, 1L, third, first, second);
                        Long seatId = 1L;

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seatId))
                                        .thenReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.unassignSeatOfConcertSchedule(concertSchedule.getId(),
                                                        seatId))
                                        .isInstanceOf(UnavailableRequestException.class);
                }

                @Test
                @DisplayName("실패 : 존재하지 않는 ConcertSchedule에 좌석을 배정 해제하려 하면 UnavailableRequestException이 발생하며 실패한다.")
                void shouldThrowUnavailableRequestException_WhenConcertScheduleNotFoundDuringUnassignment() {
                        // given
                        Long concertScheduleId = 1L;
                        Long seatId = 1L;

                        when(concertScheduleRepository.findById(concertScheduleId))
                                        .thenReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.unassignSeatOfConcertSchedule(concertScheduleId, seatId))
                                        .isInstanceOf(UnavailableRequestException.class);
                }

                @Test
                @DisplayName("실패 : 이미 배정 해제된 좌석을 다시 배정 해제하려 하면 BusinessRuleViolation 발생하며 실패한다.")
                void shouldThrowIllegalStateException_WhenSeatAlreadyUnassigned() {
                        // given
                        ConcertSchedule concertSchedule = ConcertSchedule.create(1L, 1L, third, first, second);
                        Seat seat = Seat.create(1L, 1, 1000, SeatStatus.AVAILABLE);

                        when(concertScheduleRepository.findById(concertSchedule.getId()))
                                        .thenReturn(Optional.of(concertSchedule));
                        when(seatRepository.findById(seat.getId()))
                                        .thenReturn(Optional.of(seat));

                        // when & then
                        assertThatThrownBy(
                                        () -> concertService.unassignSeatOfConcertSchedule(concertSchedule.getId(),
                                                        seat.getId()))
                                        .isInstanceOf(BusinessRuleViolationException.class);
                }
        }

        @Nested
        class GetAvailableConcertScheduleWithConcertUnitTest {
                @Test
                @DisplayName("성공 : 예약 가능한 공연 일정과 공연 정보를 함께 조회한다.")
                void shouldReturnConcertSchedulesWithConcert_WhenAvailableSchedulesExist() {
                        // given
                        LocalDateTime now = LocalDateTime.now();
                        Concert concert = Concert.create("OASIS REUNION", "oasis");
                        ConcertSchedule schedule = ConcertSchedule.create(concert.getId(), now.plusDays(1), now,
                                        now.plusDays(1));

                        when(concertScheduleRepository.findAllAvailable(any(LocalDateTime.class)))
                                        .thenReturn(List.of(schedule));
                        when(concertRepository.findAllById(anyList()))
                                        .thenReturn(List.of(concert));

                        // when
                        List<ConcertScheduleWithConcert> actual = concertService
                                        .getAvailableConcertScheduleWithConcert(now);

                        // then
                        assertEquals(1, actual.size());
                        assertEquals(schedule, actual.get(0).concertSchedule());
                        assertEquals(concert, actual.get(0).concert());
                }

                @Test
                @DisplayName("성공 : 공연 정보가 없는 공연 일정은 조회 결과에서 제외된다.")
                void shouldFilterOutSchedules_WhenConcertNotFound() {
                        // given
                        LocalDateTime now = LocalDateTime.now();
                        Concert concert = Concert.create("OASIS REUNION", "oasis");
                        ConcertSchedule schedule1 = ConcertSchedule.create(concert.getId(), now.plusDays(1), now,
                                        now.plusDays(1));
                        ConcertSchedule schedule2 = ConcertSchedule.create(2L, now.plusDays(1), now, now.plusDays(1)); // Concert
                                                                                                                       // not
                                                                                                                       // found

                        when(concertScheduleRepository.findAllAvailable(any(LocalDateTime.class)))
                                        .thenReturn(List.of(schedule1, schedule2));
                        when(concertRepository.findAllById(anyList()))
                                        .thenReturn(List.of(concert));

                        // when
                        List<ConcertScheduleWithConcert> actual = concertService
                                        .getAvailableConcertScheduleWithConcert(now);

                        // then
                        assertEquals(1, actual.size());
                        assertEquals(schedule1, actual.get(0).concertSchedule());
                        assertEquals(concert, actual.get(0).concert());
                }
        }

}