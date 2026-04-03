package com.slam.concertreservation.domain.payment.model;

import io.hypersistence.tsid.TSID;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 결제 도메인 모델
 * <br></br>
 * 결제 도메인은 사용자의 결제 요청을 처리하고, 결제 상태를 관리하는 역할을 담당합니다.
 * <br></br>
 * - Payment 데이터의 DB 저장은 도메인 성격을 고려하여 Append-Only 방식을 채택합니다.
 * <br></br>
 * - 따라서 결제 상태 변경 시 기존 데이터를 수정하지 않고, 새로운 Payment 레코드를 생성하여 상태를 기록합니다.
 * <br></br>
 * - 정적 팩토리 메서드를 활용해, 각 생성 시나리오 별로 명확한 인스턴스 생성을 지원합니다.
 * <br></br>
 * - 결제 정보 특성 상, 의도하지 않은 상태 변경을 방지 및 Append-only 특성에 맞게 상태 변경은 Setter 배제하고 새로운 인스턴스 생성을 통해서만 이루어지도록 설계되었습니다.
 */
@Getter
public class Payment {
    private Long paymentId;
    private Long userId;
    private Long reservationId;
    private int price;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    private Payment() {}
    private Payment(
            Long paymentId,
            Long userId,
            Long reservationId,
            int price,
            PaymentStatus paymentStatus,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.reservationId = reservationId;
        this.price = price;
        this.paymentId = paymentId;
        this.status = paymentStatus;
        this.createdAt = createdAt;
    }

    /**
     * 결제 요청에 대한 새로운 Payment 인스턴스를 생성
     * @param userId
     * @param price
     * @param reservationId
     * @return
     */
    public static Payment create(Long userId, int price, Long reservationId) {
        Payment payment = new Payment();
        payment.paymentId = TSID.fast().toLong();
        payment.userId = userId;
        payment.price = price;
        payment.reservationId = reservationId;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }

    /**
     * DB로부터 조회한 Payment JPA Entity -> Payment 도메인 모델로 복원
     * @param paymentId
     * @param userId
     * @param price
     * @param reservationId
     * @param status
     * @param createdAt
     * @return
     */
    public static Payment restore(
            Long paymentId,
            Long userId,
            Long reservationId,
            int price,
            PaymentStatus status,
            LocalDateTime createdAt)
    {
        Payment payment = new Payment();
        payment.paymentId = paymentId;
        payment.userId = userId;
        payment.price = price;
        payment.reservationId = reservationId;
        payment.status = status;
        payment.createdAt = createdAt;
        return payment;
    }

    /**
     * 결제 상태 변경 시, 새로운 Payment 인스턴스를 반환
     * @param paymentStatus
     * @return
     */
    public Payment withStatus(PaymentStatus paymentStatus) {
        return new Payment(this.paymentId, this.userId, this.reservationId, this.price, paymentStatus, this.createdAt);
    }
}
