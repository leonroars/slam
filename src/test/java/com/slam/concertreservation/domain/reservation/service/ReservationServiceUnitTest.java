package com.slam.concertreservation.domain.reservation.service;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.model.ReservationStatus;
import com.slam.concertreservation.domain.reservation.repository.ReservationRepository;
import com.slam.concertreservation.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RecordApplicationEvents
public class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ApplicationEvents applicationEvents;

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @TestConfiguration
    static class MockitoEventPublisherConfiguration {
        @Bean
        @Primary
        public ApplicationEventPublisher publisher() {
            return mock(ApplicationEventPublisher.class);
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    class CreateReservationTests {

        @Test
        @DisplayName("성공 : 중복이 아닌 예약을 생성하여 저장할 경우 저장된 예약을 반환한다.")
        void shouldReturnSavedReservation_WhenCreateReservation() {
            // given
            String userId = "user1";
            String concertScheduleId = "schedule1";
            String seatId = "seat1";
            int price = 1000;

            Reservation reservation = Reservation.create(String.valueOf(1L), userId, seatId, concertScheduleId, price, LocalDateTime.now().plusMinutes(6), LocalDateTime.now());

            when(reservationRepository.findByConcertScheduleIdAndSeatId(concertScheduleId, seatId))
                    .thenReturn(Optional.empty());
            when(reservationRepository.save(any(Reservation.class)))
                    .thenReturn(reservation);

            // when
            Reservation result = reservationService.createReservation(userId, concertScheduleId, seatId, price);

            // then
            verify(reservationRepository, times(1)).findByConcertScheduleIdAndSeatId(concertScheduleId, seatId);
            verify(reservationRepository, times(2)).save(any(Reservation.class));
            assertEquals(reservation.getId(), result.getId());
            assertEquals(ReservationStatus.BOOKED, result.getStatus());
        }

        @Test
        @DisplayName("실패 : 이미 예약 확정되었거나 가예약 진행 중인 좌석을 예약하려 하면 UnavailableRequestException 이 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenSeatAlreadyReserved() {
            // given
            String userId = "user1";
            String concertScheduleId = "schedule1";
            String seatId = "seat1";
            int price = 1000;

            Reservation existingReservation = Reservation.create("1", "user2", seatId, concertScheduleId, price);
            existingReservation.reserve(); // Status PAID

            when(reservationRepository.findByConcertScheduleIdAndSeatId(concertScheduleId, seatId))
                    .thenReturn(Optional.of(existingReservation));

            // when & then
            Assertions.assertThatThrownBy(() -> reservationService.createReservation(userId, concertScheduleId, seatId, price))
                    .isInstanceOf(UnavailableRequestException.class)
                    .hasMessage("해당 좌석에 대한 예약이 이미 존재하므로 예약이 불가합니다.");

            verify(reservationRepository, times(1)).findByConcertScheduleIdAndSeatId(concertScheduleId, seatId);
            verify(reservationRepository, never()).save(any(Reservation.class));
        }
    }
    @Nested
    @DisplayName("getReservation 메서드 테스트")
    class GetReservationTests {

        @Test
        @DisplayName("성공 : 존재하는 예약 ID로 조회하면 해당 예약을 반환한다.")
        void shouldReturnReservation_WhenReservationExists() {
            // given
            String reservationId = "1";
            int price = 1000;
            Reservation reservation = Reservation.create(reservationId, "user1", "seat1", "schedule1", price);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            // when
            Reservation result = reservationService.getReservation(reservationId);

            // then
            verify(reservationRepository, times(1)).findById(reservationId);
            assertEquals(reservationId, result.getId());
            assertEquals(ReservationStatus.BOOKED, result.getStatus());
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 예약 ID로 조회하려 하면 UnavailableRequestException이 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenReservationNotFound() {
            // given
            String reservationId = "unknown";

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

            // when & then
            UnavailableRequestException exception = assertThrows(UnavailableRequestException.class, () ->
                    reservationService.getReservation(reservationId));

            assertEquals("해당 예약이 존재하지 않습니다.", exception.getMessage());
            verify(reservationRepository, times(1)).findById(reservationId);
        }
    }

    @Nested
    class GetUserReservationTests {

        @Test
        @DisplayName("성공 : 존재하는 사용자의 예약을 조회하면 예약 목록을 반환한다.")
        void shouldReturnReservations_WhenUserHasReservations() {
            // given
            String userId = "user1";
            int price = 1000;
            Reservation reservation1 = Reservation.create("1", userId, "seat1", "schedule1", price);
            Reservation reservation2 = Reservation.create("2", userId, "seat2", "schedule1", price);
            List<Reservation> reservations = Arrays.asList(reservation1, reservation2);

            when(reservationRepository.findByUserId(userId)).thenReturn(reservations);

            // when
            List<Reservation> result = reservationService.getUserReservation(userId);

            // then
            verify(reservationRepository, times(1)).findByUserId(userId);
            assertEquals(2, result.size());
            assertEquals("1", result.get(0).getId());
            assertEquals("2", result.get(1).getId());
        }

        @Test
        @DisplayName("실패 : 사용자의 예약이 없으면 UnavailableRequestException이 발생하며 실패한다.")
        void shouldThrowUnavailableRequestException_WhenUserHasNoReservations() {
            // given
            String userId = "user1";

            when(reservationRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // when & then
            Assertions.assertThatThrownBy(() -> reservationService.getUserReservation(userId))
                    .isInstanceOf(UnavailableRequestException.class)
                    .hasMessage("해당 사용자의 예약이 존재하지 않습니다.");
            verify(reservationRepository, times(1)).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("cancelReservation 메서드 테스트")
    class CancelReservationTests {

        @Test
        @DisplayName("성공 : 유효한 예약을 취소하면 상태가 CANCELLED로 변경된 예약을 반환한다.")
        void shouldCancelReservation_WhenValidReservation() {
            // given
            String reservationId = "1";
            int price = 1000;
            Reservation reservation = Reservation.create(reservationId, "user1", "seat1", "schedule1", price);
            reservation.reserve(); // Status PAID

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(reservation)).thenReturn(reservation);

            // when
            Reservation result = reservationService.cancelReservation(reservationId);

            // then
            verify(reservationRepository, times(1)).findById(reservationId);
            verify(reservationRepository, times(1)).save(reservation);
            assertEquals(ReservationStatus.CANCELLED, result.getStatus());
        }

        @Test
        @DisplayName("실패 : 예약 상태가 PAID가 아니면 BusinessRuleViolationException이 발생하며 실패한다.")
        void shouldThrowBusinessRuleViolationException_WhenReservationNotPaid() {
            // given
            String reservationId = "1";
            int price = 1000;
            Reservation reservation = Reservation.create(reservationId, "user1", "seat1", "schedule1", price);
            // Status BOOKED

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            // when & then
            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () ->
                    reservationService.cancelReservation(reservationId));

            assertEquals("취소 처리는 오직 PAID 상태의 예약에 대해서만 가능합니다.", exception.getMessage());
            verify(reservationRepository, times(1)).findById(reservationId);
            verify(reservationRepository, never()).save(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("confirmReservation 메서드 테스트")
    class ConfirmReservationTests {

        @Test
        @DisplayName("성공 : 유효한 예약을 확정하면 상태가 PAID로 변경된 예약을 반환한다.")
        void shouldConfirmReservation_WhenValidReservation() {
            // given
            String reservationId = "1";
            int price = 1000;
            Reservation reservation = Reservation.create(reservationId, "user1", "seat1", "schedule1", price);
            // Status BOOKED

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(reservation)).thenReturn(reservation);

            // when
            Reservation result = reservationService.confirmReservation(reservationId);

            // then
            verify(reservationRepository, times(1)).save(reservation);
            assertEquals(ReservationStatus.PAID, result.getStatus());
        }

        @Test
        @DisplayName("실패 : 예약 상태가 BOOKED가 아니면 BusinessRuleViolationException이 발생하며 실패한다.")
        void shouldThrowBusinessRuleViolationException_WhenReservationNotBooked() {
            // given
            String reservationId = "1";
            int price = 1000;
            Reservation reservation = Reservation.create(reservationId, "user1", "seat1", "schedule1", price);
            reservation.reserve(); // Status PAID

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            when(reservationRepository.save(reservation)).thenReturn(reservation);
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

            // when & then
            Assertions.assertThatThrownBy(() -> reservationService.confirmReservation(reservationId))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(reservationRepository, never()).save(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("getReservationsToBeExpired 메서드 테스트")
    class GetReservationsToBeExpiredTests {

        @Test
        @DisplayName("성공 : 만료 대상인 예약 목록을 반환한다.")
        void shouldReturnReservationsToBeExpired_WhenReservationsExist() {
            // given
            int price = 1000;
            Reservation reservation1 = Reservation.create("1", "user1", "seat1", "schedule1", price);
            Reservation reservation2 = Reservation.create("2", "user2", "seat2", "schedule1", price);

            // 두 예약 만료 처리. 상태는 여전히 BOOKED
            reservation1.initiateExpiredAt(LocalDateTime.now());
            reservation2.initiateExpiredAt(LocalDateTime.now());

            List<Reservation> reservationsToExpire = Arrays.asList(reservation1, reservation2);

            when(reservationRepository.findAllByExpirationCriteria())
                    .thenReturn(reservationsToExpire);

            // when
            List<Reservation> result = reservationService.getReservationsToBeExpired();

            // then
            verify(reservationRepository, times(1)).findAllByExpirationCriteria();
            assertEquals(2, result.size());
            assertTrue(result.contains(reservation1));
            assertTrue(result.contains(reservation2));
        }

        @Test
        @DisplayName("성공 : 만료 대상인 예약이 없으면 빈 목록을 반환한다.")
        void shouldReturnEmptyList_WhenNoReservationsToExpire() {
            // given
            when(reservationRepository.findAllByExpirationCriteria())
                    .thenReturn(Collections.emptyList());

            // when
            List<Reservation> result = reservationService.getReservationsToBeExpired();

            // then
            verify(reservationRepository, times(1)).findAllByExpirationCriteria();
            assertTrue(result.isEmpty());
        }
    }
}