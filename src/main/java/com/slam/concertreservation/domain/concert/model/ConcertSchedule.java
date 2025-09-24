package com.slam.concertreservation.domain.concert.model;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

/**
 * 콘서트 일정을 표현하는 도메인 모델
 */
@Getter
public class ConcertSchedule {
    private String id;
    private String concertId;
    private ConcertScheduleAvailability availability;
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

        return create(concertId, dateTime, reservationStartAt, reservationEndAt, ConcertScheduleAvailability.AVAILABLE);
    }

    // 지나치게 비즈니스 로직을 강하게 강요해서 테스트에 어려움을 겪었습니다.
    // 따라서 아래와 같이 원하는 좌석 수 만큼만 할당 받도록 하는 정적 팩토리 메서드를 정의합니다.
    public static ConcertSchedule create(String concertId
            , LocalDateTime dateTime
            , LocalDateTime reservationStartAt
            , LocalDateTime reservationEndAt
            , ConcertScheduleAvailability availability){

        evaluateConcertSchedule(dateTime, reservationStartAt, reservationEndAt);

        ConcertSchedule concertSchedule = new ConcertSchedule();
        concertSchedule.id = UUID.randomUUID().toString();
        concertSchedule.concertId = concertId;
        concertSchedule.availability = availability;
        concertSchedule.dateTime = dateTime;
        concertSchedule.reservationStartAt = reservationStartAt;
        concertSchedule.reservationEndAt = reservationEndAt;

        return concertSchedule;
    }

    public static ConcertSchedule create(String concertScheduleId, String concertId, LocalDateTime dateTime, LocalDateTime reservationStartAt, LocalDateTime reservationEndAt, ConcertScheduleAvailability availability){
        evaluateConcertSchedule(dateTime, reservationStartAt, reservationEndAt);

        ConcertSchedule concertSchedule = new ConcertSchedule();
        concertSchedule.id = concertScheduleId;
        concertSchedule.concertId = concertId;
        concertSchedule.availability = availability;
        concertSchedule.dateTime = dateTime;
        concertSchedule.reservationStartAt = reservationStartAt;
        concertSchedule.reservationEndAt = reservationEndAt;

        return concertSchedule;
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

    /**
     * 콘서트 일정의 예약 가능 여부를 'AVAILABLE'로 변경합니다.
     */
    public void makeAvailable(){
        this.availability = ConcertScheduleAvailability.AVAILABLE;
    }

    /**
     * 콘서트 일정의 예약 가능 여부를 'SOLDOUT'로 변경합니다.
     */
    public void makeSoldOut(){
        this.availability = ConcertScheduleAvailability.SOLDOUT;
    }


}
