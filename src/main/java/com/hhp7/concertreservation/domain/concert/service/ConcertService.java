package com.hhp7.concertreservation.domain.concert.service;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.repository.ConcertRepository;
import com.hhp7.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.hhp7.concertreservation.domain.concert.repository.SeatRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    /**
     * Concert 등록
     * @param concert
     * @return
     */
    public Concert registerConcert(Concert concert) {
        return concertRepository.save(concert);
    }

    /**
     * Concert ID로 조회
     * @param concertId
     * @return
     */
    public Concert getConcert(String concertId){
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연이 존재하지 않아 조회에 실파했습니다."));
    }

    /**
     * ConcertSchedule 등록 + Seat.MAX_SEAT_NUMBER 개 초기화
     * @param concertSchedule
     * @param price
     * @return
     */
    public ConcertSchedule registerConcertSchedule(ConcertSchedule concertSchedule, int price){
        // 저장된 ConcertSchedule 도메인 모델 인스턴스
        ConcertSchedule registeredConcertSchedule = concertScheduleRepository.save(concertSchedule);

        // 새롭게 등록된 공연 일정에 배정된 Seat.MAX_SEAT_NUMBER 개의 좌석 생성.
        List<Seat> initialSeats = Seat.createSeatsForNewConcertSchedule(registeredConcertSchedule.getId(), price, Seat.MAX_SEAT_NUMBER);

        // Seat.MAX_SEAT_NUMBER 개 한 번에 저장.
        seatRepository.saveAll(initialSeats);

        return registeredConcertSchedule;
    }

    /**
     * ConcertSchedule 등록 + Seat 지정 수만큼 초기화
     * @param concertSchedule
     * @param price
     * @param numOfSeats
     * @return
     */
    public ConcertSchedule registerConcertSchedule(ConcertSchedule concertSchedule, int price, int numOfSeats){

        // 저장된 ConcertSchedule 도메인 모델 인스턴스
        ConcertSchedule registeredConcertSchedule = concertScheduleRepository.save(concertSchedule);

        // 새롭게 등록된 공연 일정에 배정된 지정한 개수의 좌석 생성.
        List<Seat> initialSeats = Seat.createSeatsForNewConcertSchedule(registeredConcertSchedule.getId(), price, numOfSeats);

        // 생성된 좌석 한 번에 저장.
        seatRepository.saveAll(initialSeats);

        return registeredConcertSchedule;
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    public Seat assignSeatOfConcertSchedule(String concertScheduleId, String seatId){

        // 배정될 좌석 조회
        Seat assignedSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException("해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        assignedSeat.makeUnavailable();

        // 해당 공연 일정의 '예약 가능 좌석 수' 감소
        ConcertSchedule soldConcertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연 일정이 존재하지 않습니다."));
        soldConcertSchedule.decrementAvailableSeatCount();

        // 수정된 사항을 명시적으로 저장
        Seat resultSeat = seatRepository.save(assignedSeat);
        concertScheduleRepository.save(soldConcertSchedule);

        return resultSeat;
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정 해제
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    public Seat unassignSeatOfConcertSchedule(String concertScheduleId, String seatId){

        // 배정될 좌석 조회
        Seat assignedSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException("해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        assignedSeat.makeAvailable();

        // 해당 공연 일정의 '예약 가능 좌석 수' 증가
        ConcertSchedule unassignedConcertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연 일정이 존재하지 않습니다."));
        unassignedConcertSchedule.incrementAvailableSeatCount();

        // 수정된 사항을 명시적으로 저장
        Seat resultSeat = seatRepository.save(assignedSeat);
        concertScheduleRepository.save(unassignedConcertSchedule);

        return resultSeat;
    }
}
