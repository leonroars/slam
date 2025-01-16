package com.hhp7.concertreservation.domain.reservation.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Reservation {
    private String id;
    private String userId;
    private String seatId;
    private String concertScheduleId;
    private ReservationStatus status; // 최초 생성 시 BOOKED 상태

    // BOOKED 상태 예약의 만료 시간.
    // 만료 시간 이후에는 EXPIRED 상태로 변경됩니다.
    // 생성 즉시 저장되고, 저장 당시 createdAt 시간 기준 5분 후로 설정됩니다.
    // 따라서 Nullable 한 필드로 설정합니다.
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public static final int VALID_FOR_MINUTES = 5;

    private Reservation(){}

    /**
     * BOOKED 상태의 예약에 대한 만료 처리를 수행합니다.
     * <br></br>
     * 만료 처리는 오직 BOOKED 상태의 예약에 대해서만 가능합니다. 이외엔 {@code BusinessRuleViolationException} 발생합니다.
     * @return
     */
    public Reservation expire(){
        if(this.status != ReservationStatus.BOOKED){
            throw new BusinessRuleViolationException("만료 처리는 오직 BOOKED 상태의 예약에 대해서만 가능합니다.");
        }
        this.status = ReservationStatus.EXPIRED;
        return this;
    }

    /**
     * 예약을 취소합니다.
     * <br></br>
     * 취소 처리는 오직 PAID 상태의 예약에 대해서만 가능합니다. 이외엔 {@code BusinessRuleViolationException} 발생합니다.
     * @return
     */
    public Reservation cancel() {
        if (this.status != ReservationStatus.PAID) {
            throw new BusinessRuleViolationException("취소 처리는 오직 PAID 상태의 예약에 대해서만 가능합니다.");
        }
        this.status = ReservationStatus.CANCELLED;
        return this;
    }

    /**
     * 결제를 통해 예약을 완료합니다.
     * <br></br>
     * 완료 처리는 오직 BOOKED 상태의 예약에 대해서만 가능합니다. 이외엔 {@code BusinessRuleViolationException} 발생합니다.
     * @return
     */
    public Reservation reserve() {
        if (this.status != ReservationStatus.BOOKED) {
            throw new BusinessRuleViolationException("완료 처리는 오직 BOOKED 상태의 예약에 대해서만 가능합니다.");
        }
        this.status = ReservationStatus.PAID;
        return this;
    }

    /**
     * 현재 과제 요구사항 상 예약이 확정되지 않았을 때 만료시키기 위해 '만료 시점'을 명시하는 필드를 추가하였습니다.
     * <br></br>
     * 하지만 이를 도메인 로직 상에서 처리할 경우 실제 해당 객체의 persist가 이루어지기까지 시간 차가 발생할 수 있고,
     * <br>
     * 정확한 비즈니스 요구사항 시행이 불가하다고 판단했습니다.
     * <br></br>
     * 또한 Infrastructure level 에서 @PostPersist 와 같은 방식을 사용해보려 했으나,
     * <br>
     * 해당 방식은 비즈니스 로직이 Infrastructure level에 의존하게 되는 문제가 있다고 생각했습니다.
     * <br></br>
     * 따라서 '만료 시간 정의'를 도메인 로직으로 보고, 해당 로직을 도메인 모델 내부에 구현하여 해당 필드 수정이 가능하도록 하였습니다.
     * <br></br>
     * 이후 만료 시간이 갱신된 도메인 모델을 서비스 로직에서 한 번 더 명시적으로 저장하도록 구현할 예정입니다.
     * <br></br>
     * 이렇게 했을 때 확보할 수 있는 서비스 로직의 명료함/정확함이 두 번의 삽입 연산으로부터 발생할 수 있는 오버헤드보다 유익하다고 생각했기 때문입니다.
     * @return
     */
    public void initiateExpiredAt(){
        this.expiredAt = this.createdAt.plusMinutes(VALID_FOR_MINUTES);
    }

    /**
     * 테스트 가능성을 위해, 임의의 만료 시간을 설정할 수 있는 메서드를 추가하였습니다.
     * @param intendedTime
     */
    public void initiateExpiredAt(LocalDateTime intendedTime){
        this.expiredAt = intendedTime;
    }

    // 정적 팩토리 메서드 1 : 전체 필드 활용 생성
    public static Reservation create(String id
            , String userId
            , String seatId
            , String concertScheduleId
            , ReservationStatus status
            , LocalDateTime expiredAt, LocalDateTime createdAt){
        Reservation reservation = new Reservation();
        reservation.id = id;
        reservation.userId = userId;
        reservation.seatId = seatId;
        reservation.concertScheduleId = concertScheduleId;
        reservation.status = status;
        reservation.expiredAt = expiredAt;
        reservation.createdAt = createdAt;

        return reservation;
    }

    // 정적 팩토리 메서드 2 : status 미포함 (초기화 용도.)
    public static Reservation create(String id, String userId, String seatId, String concertScheduleId, LocalDateTime expiredAt, LocalDateTime createdAt){
        return create(id, userId, seatId, concertScheduleId, ReservationStatus.BOOKED, expiredAt, createdAt);
    }

    // 정적 팩토리 메서드 2 : ID, 만료시간 미포함
    public static Reservation create(String userId, String seatId, String concertScheduleId){
        return create(null, userId, seatId, concertScheduleId, null, null);
    }

    // 정적 팩토리 메서드 3 : ID 미포함, 만료시간 포함
    public static Reservation create(String userId, String seatId, String concertScheduleId, LocalDateTime expiredAt){
        return create(null, userId, seatId, concertScheduleId, expiredAt, null);
    }

    // 정적 팩토리 메서드 4: ID 포함, 만료시간 미포함
    public static Reservation create(String id, String userId, String seatId, String concertScheduleId){
        return create(id, userId, seatId, concertScheduleId, null, null);
    }
}
