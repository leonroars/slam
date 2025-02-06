package com.hhp7.concertreservation.domain.concert.service;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.repository.ConcertRepository;
import com.hhp7.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.hhp7.concertreservation.domain.concert.repository.SeatRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
     * ConcertSchedule ID로 조회
     * @param concertScheduleId
     * @return
     */
    public ConcertSchedule getConcertSchedule(String concertScheduleId) {
        return concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연 일정이 존재하지 않습니다."));
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
     * 특정 ConcertSchedule의 선점된 좌석 수 조회
     * @param concertScheduleId
     * @return
     */
    public int getOccupiedSeatsCount(String concertScheduleId){
        return seatRepository.findOccupiedSeatsCount(concertScheduleId);
    }

    /**
     * 특정 ConcertSchedule의 남은 좌석 수 조회
     * @param concertScheduleId
     * @return
     */
    public int getRemainingSeatsCount(String concertScheduleId){
        return ConcertSchedule.MAX_AVAILABLE_SEATS - getOccupiedSeatsCount(concertScheduleId);
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Seat assignSeatOfConcertSchedule(String concertScheduleId, String seatId){

        // 배정될 좌석 조회
        Seat assignedSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException("해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        assignedSeat.makeUnavailable();

        // 수정된 사항을 명시적으로 저장 및 반환
        return seatRepository.save(assignedSeat);
    }

    /**
     * 특정 ConcertSchedule의 상태를 AVAILABLE로 변경.
     * <br></br>
     * 해당 메서드는 이후 파사드에서 좌석 예약 직후 호출되며, 두 메서드는 분리된 트랜잭션으로 처리됩니다.
     * @param concertScheduleId
     * @return
     */
    public ConcertSchedule makeConcertScheduleAvailable(String concertScheduleId){
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연 일정이 존재하지 않습니다."));

        concertSchedule.makeAvailable();

        return concertScheduleRepository.save(concertSchedule);
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정 해제
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Seat unassignSeatOfConcertSchedule(String concertScheduleId, String seatId){

        // 배정될 좌석 조회
        Seat assignedSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException("해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        assignedSeat.makeAvailable();

        // 수정된 사항을 명시적으로 저장
        return seatRepository.save(assignedSeat);
    }

    /**
     * 특정 ConcertSchedule의 상태를 SOLDOUT으로 변경.
     * @param concertScheduleId
     * @return
     */
    public ConcertSchedule makeConcertScheduleSoldOut(String concertScheduleId){
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException("해당 공연 일정이 존재하지 않습니다."));

        concertSchedule.makeSoldOut();

        return concertScheduleRepository.save(concertSchedule);
    }

    /**
     * 특정 ConcertSchedule의 모든 좌석 조회
     * @param concertScheduleId
     * @return
     */
    public List<Seat> getSeatsOfConcertSchedule(String concertScheduleId) {
        return seatRepository.findAllByConcertScheduleId(concertScheduleId);
    }

    /**
     * 특정 좌석 조회.
     * @param seatId
     * @return
     */
    public Seat getSeat(String seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException("해당 좌석이 존재하지 않습니다."));
    }

    /**
     * 특정 ConcertSchedule의 예약 가능 좌석 조회
     * @param concertScheduleId
     * @return
     */
    public List<Seat> getAvailableSeatsOfConcertSchedule(String concertScheduleId) {
        List<Seat> availableSeats =  seatRepository.findAllAvailableSeatsByConcertScheduleId(concertScheduleId);
        if(availableSeats.isEmpty()){throw new UnavailableRequestException("예약 가능한 좌석이 존재하지 않습니다.");}
        return availableSeats;
    }

    /**
     * 예약 가능 공연 일정 목록 조회.
     * @param presentDateTime
     * @return
     */
    public List<ConcertSchedule> getAvailableConcertSchedule(LocalDateTime presentDateTime) {
        return concertScheduleRepository.findAllAvailable(presentDateTime);
    }

    /**
     * 예약 진행 중인 공연 일정 목록 조회
     * @param presentDateTime
     * @return
     */
    @Cacheable(value = "ongoingConcertSchedules", key = "'list'")
    public List<ConcertSchedule> getOngoingConcertSchedules(LocalDateTime presentDateTime) {
        return concertScheduleRepository.findAllOngoingConcertSchedules(presentDateTime);
    }
}
