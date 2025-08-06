package com.slam.concertreservation.domain.reservation.service;

import com.slam.concertreservation.domain.reservation.event.ReservationConfirmationRollbackEvent;
import com.slam.concertreservation.domain.reservation.event.ReservationExpirationRollbackEvent;
import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.repository.ReservationRepository;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * ReservationService에서 발생한 도메인 로직 트랜잭션을 롤백하는 서비스입니다.
 * <br></br>
 * 예약 취소, 만료, 취소 트랜잭션을 롤백합니다.
 * <br></br>
 * 롤백 로직이 도메인 서비스 내에 구현될 경우 책임 분리가 명확하지 않고 지저분해질 수 있을 것 같아 분리했습니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRollbackService {

    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 예약 취소를 롤백합니다.
     * @param reservationId
     */
    @Transactional
    public void rollbackConfirmReservation(String reservationId) {
        Reservation correspondingReservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new UnavailableRequestException("해당 예약이 존재하지 않아 롤백이 불가합니다."));

        correspondingReservation.rollbackReserve();
        Reservation rollbackedReservation = reservationRepository.save(correspondingReservation);

        log.warn("예약 취소 롤백 완료: {}", rollbackedReservation.getId()); // 롤백 완료 로깅.

        // 예약 확정 트랜잭션 롤백 완료 이벤트 발행.
        applicationEventPublisher.publishEvent(ReservationConfirmationRollbackEvent.fromDomain(
                rollbackedReservation.getId(),
                rollbackedReservation.getConcertScheduleId(),
                rollbackedReservation.getUserId(),
                rollbackedReservation.getSeatId()));
    }

    /**
     * 예약 만료를 롤백합니다.
     * @param reservationId
     */
    @Transactional
    public void rollbackExpireReservation(String reservationId) {
        Reservation correspondingReservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new UnavailableRequestException("해당 예약이 존재하지 않아 롤백이 불가합니다."));

        correspondingReservation.rollbackExpire();
        Reservation rollbackedReservation = reservationRepository.save(correspondingReservation);

        log.warn("예약 만료 롤백 완료: {}", rollbackedReservation.getId()); // 롤백 완료 로깅.

        // 예약 만료 트랜잭션 롤백 완료 이벤트 발행.
        applicationEventPublisher.publishEvent(ReservationExpirationRollbackEvent.fromDomain(
                rollbackedReservation.getId(),
                rollbackedReservation.getConcertScheduleId(),
                rollbackedReservation.getUserId(),
                rollbackedReservation.getSeatId()));
    }
}
