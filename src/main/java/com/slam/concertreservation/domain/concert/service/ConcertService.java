package com.slam.concertreservation.domain.concert.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.concert.model.Concert;
import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.ConcertScheduleWithConcert;
import com.slam.concertreservation.domain.concert.model.Seat;
import com.slam.concertreservation.domain.concert.repository.ConcertRepository;
import com.slam.concertreservation.domain.concert.repository.ConcertScheduleRepository;
import com.slam.concertreservation.domain.concert.repository.SeatRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.infrastructure.persistence.redis.locking.RedissonDistributedLock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Concert 등록
     * 
     * @param concert
     * @return
     */
    @Transactional
    public Concert registerConcert(Concert concert) {
        return concertRepository.save(concert);
    }

    /**
     * Concert ID로 조회
     * 
     * @param concertId
     * @return
     */
    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND,
                        "해당 공연이 존재하지 않아 조회에 실패했습니다."));
    }

    /**
     * ConcertSchedule 등록 + Seat.MAX_SEAT_NUMBER 개 초기화
     * 
     * @param concertSchedule
     * @param price
     * @return
     */
    @Transactional
    public ConcertSchedule registerConcertSchedule(ConcertSchedule concertSchedule, int price) {
        // 저장된 ConcertSchedule 도메인 모델 인스턴스
        ConcertSchedule registeredConcertSchedule = concertScheduleRepository.save(concertSchedule);

        // 새롭게 등록된 공연 일정에 배정된 Seat.MAX_SEAT_NUMBER 개의 좌석 생성.
        List<Seat> initialSeats = Seat.createSeatsForNewConcertSchedule(
                registeredConcertSchedule.getId(), price,
                Seat.MAX_SEAT_NUMBER);

        // Seat.MAX_SEAT_NUMBER 개 한 번에 저장.
        seatRepository.saveAll(initialSeats);

        return registeredConcertSchedule;
    }

    /**
     * ConcertSchedule ID로 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public ConcertSchedule getConcertSchedule(Long concertScheduleId) {
        return concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND,
                        "해당 공연 일정이 존재하지 않습니다."));
    }

    /**
     * ConcertSchedule 등록 + Seat 지정 수만큼 초기화
     * 
     * @param concertSchedule
     * @param price
     * @param numOfSeats
     * @return
     */
    @Transactional
    public ConcertSchedule registerConcertSchedule(ConcertSchedule concertSchedule, int price, int numOfSeats) {

        // 저장된 ConcertSchedule 도메인 모델 인스턴스
        ConcertSchedule registeredConcertSchedule = concertScheduleRepository.save(concertSchedule);

        // 새롭게 등록된 공연 일정에 배정된 지정한 개수의 좌석 생성.
        List<Seat> initialSeats = Seat.createSeatsForNewConcertSchedule(
                registeredConcertSchedule.getId(), price,
                numOfSeats);

        // 생성된 좌석 한 번에 저장.
        seatRepository.saveAll(initialSeats);

        return registeredConcertSchedule;
    }

    /**
     * 특정 ConcertSchedule의 선점된 좌석 수 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public int getOccupiedSeatsCount(Long concertScheduleId) {
        return seatRepository.findOccupiedSeatsCount(concertScheduleId);
    }

    /**
     * 특정 ConcertSchedule의 남은 좌석 수 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public int getRemainingSeatsCount(Long concertScheduleId) {
        return ConcertSchedule.MAX_AVAILABLE_SEATS - getOccupiedSeatsCount(concertScheduleId);
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정
     * 
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    @Transactional
    @RedissonDistributedLock(key = "seatId")
    public Seat assignSeatOfConcertSchedule(Long concertScheduleId, Long seatId, Long userId) {

        // 배정될 좌석 조회
        Seat targetSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.SEAT_NOT_FOUND, "해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        targetSeat.makeUnavailable();

        // 수정된 사항을 명시적으로 저장 및 반환
        Seat assignedSeat = seatRepository.save(targetSeat);

        log.info("좌석 선점 완료 - seatId: {}, userId: {}, concertScheduleId: {}, price: {}",
                seatId, userId, concertScheduleId, assignedSeat.getPrice());

        return assignedSeat;
    }

    /**
     * 특정 ConcertSchedule의 특정 좌석 배정 해제
     * 
     * @param concertScheduleId
     * @param seatId
     * @return
     */
    @Transactional
    @RedissonDistributedLock(key = "seatId")
    public Seat unassignSeatOfConcertSchedule(Long concertScheduleId, Long seatId) {

        // 배정될 좌석 조회
        Seat targetSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.SEAT_NOT_FOUND, "해당 좌석이 존재하지 않습니다."));

        // 배정될 좌석의 상태 변경
        targetSeat.makeAvailable();

        // 수정된 사항을 명시적으로 저장
        Seat unasignedSeat = seatRepository.save(targetSeat);

        log.warn("좌석 선점 해제 - seatId: {}, concertScheduleId: {}",
                seatId, concertScheduleId);

        return unasignedSeat;
    }

    /**
     * 특정 ConcertSchedule의 상태를 AVAILABLE로 변경.
     * <br>
     * </br>
     * 변경 전 변경 필요 상태인지 확인합니다.
     * 
     * @param concertScheduleId
     * @return
     */
    @Transactional
    public ConcertSchedule makeConcertScheduleAvailable(Long concertScheduleId) {
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND,
                        "해당 공연 일정이 존재하지 않습니다."));

        if (getRemainingSeatsCount(concertScheduleId) > 0) {
            concertSchedule.makeAvailable();
        }

        return concertScheduleRepository.save(concertSchedule);
    }

    /**
     * 특정 ConcertSchedule의 상태를 SOLDOUT으로 변경.
     * 
     * @param concertScheduleId
     * @return
     */
    @Transactional
    public ConcertSchedule makeConcertScheduleSoldOut(Long concertScheduleId) {
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.CONCERT_SCHEDULE_NOT_FOUND,
                        "해당 공연 일정이 존재하지 않습니다."));

        int remainingSeatsCount = getRemainingSeatsCount(concertScheduleId);
        log.info("남은 좌석 수 : {}", remainingSeatsCount);

        if (getRemainingSeatsCount(concertScheduleId) == 0) {
            concertSchedule.makeSoldOut();
            log.warn("공연 매진 처리 - concertScheduleId: {}", concertScheduleId);
        }

        return concertScheduleRepository.save(concertSchedule);
    }

    /**
     * 특정 ConcertSchedule의 모든 좌석 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public List<Seat> getSeatsOfConcertSchedule(Long concertScheduleId) {
        return seatRepository.findAllByConcertScheduleId(concertScheduleId);
    }

    /**
     * 특정 좌석 조회.
     * 
     * @param seatId
     * @return
     */
    public Seat getSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.SEAT_NOT_FOUND, "해당 좌석이 존재하지 않습니다."));
    }

    /**
     * 특정 ConcertSchedule의 예약 가능 좌석 조회
     * 
     * @param concertScheduleId
     * @return
     */
    public List<Seat> getAvailableSeatsOfConcertSchedule(Long concertScheduleId) {
        List<Seat> availableSeats = seatRepository
                .findAllAvailableSeatsByConcertScheduleId(concertScheduleId);
        if (availableSeats.isEmpty()) {
            throw new UnavailableRequestException(ErrorCode.NO_AVAILABLE_SEATS, "예약 가능한 좌석이 존재하지 않습니다.");
        }
        return availableSeats;
    }

    @Transactional(readOnly = true)
    public List<ConcertScheduleWithConcert> getAvailableConcertScheduleWithConcert(LocalDateTime presentDateTime) {

        // 예약 가능한 공연 일정 조회
        List<ConcertSchedule> schedules = concertScheduleRepository.findAllAvailable(presentDateTime);

        // 예약 가능한 공연 ID 목록 추출
        List<Long> concertIds = schedules.stream()
                .map(ConcertSchedule::getConcertId)
                .distinct()
                .toList();

        // 공연 ID 목록으로 공연 조회
        Map<Long, Concert> concerts = concertRepository.findAllById(concertIds)
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(Concert::getId, concert -> concert));

        // ConcertSchedule과 Concert를 결합하여 ConcertScheduleWithConcert 생성
        return schedules.stream()
                .filter(schedule -> concerts.get(schedule.getConcertId()) != null)
                .map(schedule -> new ConcertScheduleWithConcert(
                        schedule,
                        concerts.get(schedule.getConcertId())))
                .toList();
    }

    /**
     * 예약 진행 중인 공연 일정 목록 조회
     * 
     * @param presentDateTime
     * @return
     */
    public List<ConcertSchedule> getOngoingConcertSchedules(LocalDateTime presentDateTime) {
        return concertScheduleRepository.findAllOngoingConcertSchedules(presentDateTime);
    }
}
