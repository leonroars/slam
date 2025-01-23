package com.hhp7.concertreservation.application.facade;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.point.model.PointHistory;
import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import com.hhp7.concertreservation.domain.point.service.PointService;
import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.service.QueueService;
import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.service.ReservationService;
import com.hhp7.concertreservation.domain.user.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ConcertReservationApplication {

    private final ConcertService concertService;
    private final PointService pointService;
    private final QueueService queueService;
    private final ReservationService reservationService;

    /**
     * 사용자의 포인트 잔액을 조회합니다.
     * @param userId
     * @return
     */
    @Transactional
    public UserPointBalance getUserPointBalance(String userId){
        return pointService.getUserPointBalance(userId);
    }

    /**
     * 사용자의 포인트를 충전합니다.
     * @param userId
     * @param amount
     * @return
     */
    @Transactional
    public UserPointBalance chargeUserPoint(String userId, int amount){
        return pointService.increaseUserPointBalance(userId, amount);
    }

    /**
     * 사용자의 포인트를 사용합니다.
     * @param userId
     * @param amount
     * @return
     */
    @Transactional
    public UserPointBalance useUserPoint(String userId, int amount) {
        return pointService.decreaseUserPointBalance(userId, amount);
    }

    /**
     * 사용자 포인트 사용/충전 내역 전체 조회
     * @param userId
     * @return
     */
    public List<PointHistory> getPointHistories(String userId){
        return pointService.getUserPointHistories(userId);
    }

    // 공연 등록
    public Concert registerConcert(String name, String artist) {
        return concertService.registerConcert(Concert.create(name, artist));
    }

    // 공연 조회
    public Concert getConcert(String concertId) {
        return concertService.getConcert(concertId);
    }


    // 공연 일정 등록
    @Transactional
    public ConcertSchedule registerConcertSchedule(String concertId
            , LocalDateTime concertDateTime
            , LocalDateTime reservationStartAt
            , LocalDateTime reservationEndAt
            , int price) {
        return concertService.registerConcertSchedule(ConcertSchedule.create(concertId, concertDateTime, reservationStartAt, reservationEndAt), price);
    }

    // 공연 일정 조회
    public ConcertSchedule getConcertSchedule(String concertScheduleId) {
        return concertService.getConcertSchedule(concertScheduleId);
    }


    /**
     * 해당 메서드 호출 시점 기준 예약 가능 공연 일정 목록 조회
     * @return
     */
    @Transactional
    public List<ConcertSchedule> getAvailableConcertSchedules(){
        return concertService.getAvailableConcertSchedule(LocalDateTime.now());
    }

    /**
     * 예약하고자 하는 공연의 예약 가능 좌석 전체 목록 조회.
     * @param concertScheduleId
     * @return
     */
    @Transactional
    public List<Seat> getAvailableSeats(String concertScheduleId){
        return concertService.getAvailableSeatsOfConcertSchedule(concertScheduleId);
    }

    /**
     * 특정 좌석 조회
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    @Transactional
    public Seat getSeat(String seatId){
        return concertService.getSeat(seatId);
    }

    /**
     * 특정 공연 일정의 특정 좌석에 대한 가예약을 요청합니다.
     * @param concertScheduleId
     * @param userId
     * @param seatId
     */
    @Transactional
    public Reservation createTemporaryReservation(String concertScheduleId, String userId, String seatId){

        // 가예약 생성
        Reservation createdReservation = reservationService.createReservation(userId, concertScheduleId, seatId);

        // 해당 좌석 할당 처리(AVAILABLE -> UNAVAILABLE)
        Seat assignedSeat = concertService.assignSeatOfConcertSchedule(concertScheduleId, seatId);

        return createdReservation;
    }

    /**
     * 특정 가예약에 대한 예약 확정 절차를 진행합니다. 결제 과정을 포함합니다. (추후 ReservationId만 받도록 리팩터 예정.)
     * @param concertScheduleId
     * @param userId
     * @param seatId
     * @return
     */
    @Transactional
    public Reservation confirmReservation(String concertScheduleId, String userId, String seatId){

        // 해당 공연 좌석에 대한 해당 사용자 가예약 조회
        Reservation tempReservation = reservationService.getReservationByConcertScheduleIdAndUserId(concertScheduleId, userId);

        // 결제
        pointService.decreaseUserPointBalance(userId, concertService.getSeat(seatId).getPrice());

        // 가예약 확정 처리
        return reservationService.confirmReservation(tempReservation.getId());
    }

    /**
     * 예약 취소
     * @param reservationId
     */
    @Transactional
    public Reservation cancelReservation(String reservationId) {
        // 예약 취소
        Reservation canceledReservation = reservationService.cancelReservation(reservationId);

        // 해당 좌석 할당 해제(AVAILABLE -> UNAVAILABLE) 및 가격 조회.
        int seatPrice = concertService.assignSeatOfConcertSchedule(canceledReservation.getConcertScheduleId(),
                canceledReservation.getSeatId()).getPrice();

        // 사용자 포인트 환불
        pointService.increaseUserPointBalance(canceledReservation.getUserId(), seatPrice);

        return canceledReservation;
    }

    /**
     * 예약 조회
     * @param reservationId
     * @return
     */
    public Reservation getReservation(String reservationId) {
        return reservationService.getReservation(reservationId);
    }

    /**
     * 대기열 진입 사용자에 대해 토큰 발급.
     * @param userId
     * @param concertScheduleId
     * @return
     */
    public Token issueToken(String userId, String concertScheduleId){
        return queueService.issueToken(userId, concertScheduleId);
    }

    /**
     * 대기열 폴링 시에, 진입까지 남은 사용자 수 반환.
     * @param concertScheduleId
     * @param tokenId
     * @return
     */
    @Transactional
    public int getRemaining(String concertScheduleId, String tokenId){
        return queueService.getRemainingTokenCount(concertScheduleId, tokenId);
    }
}
