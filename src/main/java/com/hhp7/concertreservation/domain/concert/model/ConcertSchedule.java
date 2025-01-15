package com.hhp7.concertreservation.domain.concert.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 콘서트 일정을 표현하는 도메인 모델
 */
@Getter
public class ConcertSchedule {
    private String id;
    private String concertId;
    private int availableSeatCount;
    private LocalDateTime dateTime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;

    /*
     공연 일정에 대한 도메인 규칙.
     이를 public으로 함으로써 코드 작성 시 도메인 규칙을 확인하기 용이하게 한다.
     이러한 변경은 현재 코드가 Testable 하도록 만들어준다.
    */
    public static final int MAX_AVAILABLE_SEATS = 50;
    public static final int MIN_AVAILABLE_SEATS = 0;

    private ConcertSchedule() {}

    // ID 포함 정적 팩토리 메서드.
    public static ConcertSchedule create(String id, String concertId, LocalDateTime dateTime, LocalDateTime reservationStartAt, LocalDateTime reservationEndAt){

        // 콘서트 일정 타당성 검증. 타당하지 않은 경우 콘서트 일정 도메인 모델의 객체 인스턴스 생성 자체가 불가합니다.
        evaluateConcertSchedule(dateTime, reservationStartAt, reservationEndAt);

        ConcertSchedule concertSchedule = create(concertId, dateTime, reservationStartAt, reservationEndAt);
        concertSchedule.id = id;

        return concertSchedule;
    }

    // ID 미포함 정적 팩토리 메서드.
    public static ConcertSchedule create(String concertId, LocalDateTime dateTime, LocalDateTime reservationStartAt, LocalDateTime reservationEndAt){

        return create(concertId, dateTime, reservationStartAt, reservationEndAt, MAX_AVAILABLE_SEATS);
    }

    // 지나치게 비즈니스 로직을 강하게 강요해서 테스트에 어려움을 겪었습니다.
    // 따라서 아래와 같이 원하는 좌석 수 만큼만 할당 받도록 하는 정적 팩토리 메서드를 정의합니다.
    public static ConcertSchedule create(String concertId
            , LocalDateTime dateTime
            , LocalDateTime reservationStartAt
            , LocalDateTime reservationEndAt
            , int availableSeatCount){

        evaluateConcertSchedule(dateTime, reservationStartAt, reservationEndAt);

        ConcertSchedule concertSchedule = new ConcertSchedule();
        concertSchedule.id = UUID.randomUUID().toString();
        concertSchedule.concertId = concertId;
        concertSchedule.availableSeatCount = availableSeatCount;
        concertSchedule.dateTime = dateTime;
        concertSchedule.reservationStartAt = reservationStartAt;
        concertSchedule.reservationEndAt = reservationEndAt;

        return concertSchedule;
    }

    /**
     * 해당 공연 일정의 예약 가능 좌석 수를 증가시킨다.
     * <br></br>
     * 비즈니스 로직 상 이는 '이미 체결된 예약의 취소' 혹은 '가예약 상태 유지기간 만료' 상황에서 발생한다.
     * <br></br>
     * 또한 시나리오 상 각 공연별 할당 최대 좌석 수는 50석이므로, 이를 위반 가능케하는 시도 발생 시 {@code BusinessRuleViolation} 예외를 발생시켜 해당 가능성을 차단한다.
     */
    public ConcertSchedule incrementAvailableSeatCount(){
        if(availableSeatCount == MAX_AVAILABLE_SEATS){
            throw new BusinessRuleViolationException("콘서트 일정 별로 할당된 좌석은 최대 50석이므로 추가 할당이 불가합니다.");
        }
        availableSeatCount++;

        return this;
    }

    /**
     * 해당 공연 일정의 예약 가능 좌석 수를 감소시킨다.
     * <br></br>
     * 비즈니스 로직 상 이는 해당 공연 일정에 대해 '예약 확정' 혹은 '가예약 체결' 상황에서 발생한다.
     * <br></br>
     * 또한 예약 가능 좌석 수가 0석일 경우 매진이므로, 이를 위반 가능케하는 시도 발생 시 {@code BusinessRuleViolation} 예외를 발생시켜 해당 가능성을 차단한다.
     */
    public ConcertSchedule decrementAvailableSeatCount(){
        if(availableSeatCount == MIN_AVAILABLE_SEATS){
            throw new BusinessRuleViolationException("해당 콘서트 일정의 잔여 좌석이 존재하지 않으므로 예약 가능 좌석 수 차감이 불가합니다.");
        }
        availableSeatCount--;
        return this;
    }


    /**
     * 콘서트 타당성 검증을 제공하는 메서드입니다.
     * <br></br>
     * 성립 불가한 공연 일정, 예약 시작 일자, 예약 종료 일자가 아닌 지 검증하고 성립 불가할 시 {@code BusinessRuleViolationException} 발생합니다.
     * <br></br>
     * DRY 원칙 준수를 위해 시험적으로 도입해보았습니다!
     * @param dateTime
     * @param reservationStartAt
     * @param reservationEndAt
     */
    private static void evaluateConcertSchedule(LocalDateTime dateTime
            , LocalDateTime reservationStartAt
            , LocalDateTime reservationEndAt)
    {
        if(reservationStartAt.isAfter(reservationEndAt)){
            throw new BusinessRuleViolationException("예약 시작 일자는 예약 종료 일자보다 늦을 수 없습니다.");
        }
        if(dateTime.isBefore(reservationStartAt)){
            throw new BusinessRuleViolationException("공연 일자가 예약 가능 시작 일자에 선행합니다.");
        }
        if(dateTime.isBefore(reservationEndAt)){
            throw new BusinessRuleViolationException("예약 가능 기간 중엔 공연이 종료될 수 없습니다.");
        }
    }

}
