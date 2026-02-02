package com.slam.concertreservation.domain.reservation.event;

import java.time.LocalDateTime;

/**
 * 예약 만료 작업이 롤백되었음을 알리는 이벤트.
 * <br>
 * </br>
 * 이는 예약 만료에 따른 좌석 상태 변경(선점 -> 예약 가능) 작업이 실패하였을 때 발생한다.
 * <br>
 * </br>
 * Listener : 해당 이벤트 청취 시 공연 일정 예약 가능 상태 변경 필요 여부 확인 후 변경한다.(매진 -> 예약 가능)
 * 
 * @param reservationId
 * @param concertScheduleId
 * @param userId
 * @param seatId
 * @param failedAt
 */
public record ReservationExpirationRollbackEvent(
        Long reservationId,
        Long concertScheduleId,
        Long userId,
        Long seatId,
        LocalDateTime failedAt) {
    public static ReservationExpirationRollbackEvent fromDomain(Long reservationId, Long concertScheduleId,
            Long userId, Long seatId) {
        return new ReservationExpirationRollbackEvent(
                reservationId,
                concertScheduleId,
                userId,
                seatId,
                LocalDateTime.now());
    }
}
