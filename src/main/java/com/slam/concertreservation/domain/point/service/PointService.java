package com.slam.concertreservation.domain.point.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.domain.point.event.PaymentEvent;
import com.slam.concertreservation.domain.point.model.Point;
import com.slam.concertreservation.domain.point.model.PointHistory;
import com.slam.concertreservation.domain.point.model.PointTransactionType;
import com.slam.concertreservation.domain.point.model.UserPointBalance;
import com.slam.concertreservation.domain.point.repository.PointHistoryRepository;
import com.slam.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointBalanceRepository userPointBalanceRepository;

    @Transactional
    public UserPointBalance processPaymentForReservation(String userId, int price, String reservationId){
        UserPointBalance processedUserPointBalance = decreaseUserPointBalance(userId, price);

        applicationEventPublisher.publishEvent(new PaymentEvent(userId, price, reservationId));
        return processedUserPointBalance;
    }

    /**
     * 특정 사용자의 포인트 잔액을 감액한 후 변동된 잔액을 저장하고 이를 반환한다.
     * <br></br>
     * 또한, 이에 대한 내역을 생성하여 저장한다.
     *
     * @param userId 사용자 ID
     * @param decreaseAmount 감액량
     * @return 변동된 사용자의 잔액
     */
    @Transactional
    public UserPointBalance decreaseUserPointBalance(String userId, int decreaseAmount){

        UserPointBalance userPointBalance = userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.USER_NOT_FOUND, "해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));;
        UserPointBalance updatedUserPointBalance = userPointBalance.decrease(decreaseAmount);
        PointHistory pointHistory = PointHistory.create(
                userId,
                PointTransactionType.USE,
                decreaseAmount
        );
        // 포인트 내역 저장
        pointHistoryRepository.save(pointHistory);
        // 차감 후의 포인트 잔액 저장
        UserPointBalance decreasedUserPoint = userPointBalanceRepository.save(updatedUserPointBalance);

        // 포인트 감소 이벤트 발행. 만약 롤백 발생 시 사용자 ID 와 금액을 전달하는 이벤트를 발행한다.
        applicationEventPublisher.publishEvent(UserPointBalance.create(userId, Point.create(decreaseAmount)));

        return decreasedUserPoint;
    }

    /**
     * 특정 사용자의 포인트 잔액을 증액한 후 변동된 잔액을 저장하고 이를 반환한다.
     * <br></br>
     * 또한, 이에 대한 내역을 생성하여 저장한다.
     *
     * @param userId 사용자 ID
     * @param increaseAmount 증액량
     * @return 변동된 사용자의 잔액
     */
    @Transactional
    public UserPointBalance increaseUserPointBalance(String userId, int increaseAmount){
        UserPointBalance userPointBalance = userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.USER_NOT_FOUND, "해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));
        UserPointBalance updatedUserPointBalance = userPointBalance.increase(increaseAmount);
        PointHistory pointHistory = PointHistory.create(
                userId,
                PointTransactionType.CHARGE,
                increaseAmount
        );
        pointHistoryRepository.save(pointHistory);
        return userPointBalanceRepository.save(updatedUserPointBalance);

    }

    /**
     * 신규 사용자의 포인트 잔액과 이에 대한 내역을 생성 및 저장합니다.
     * <br></br>
     * 잔액 0인 UserPointBalance를 반환합니다.
     * <br></br>
     * 해당 사용자 존재하지 않을 경우 {@code BusinessRuleViolationException} 예외가 발생합니다.
     * @param userId 사용자 ID
     * @return 잔액 0인 사용자 잔액
     * */
    @Transactional
    public UserPointBalance createUserPointBalance(String userId){
        UserPointBalance userPointBalance = UserPointBalance.create(userId, Point.create(0));
        pointHistoryRepository.save(PointHistory.create(userId, PointTransactionType.INIT, 0));
        return userPointBalanceRepository.save(userPointBalance);
    }

    /**
     * 특정 사용자의 잔액 조회
     * <br></br>
     * 해당 사용자 존재하지 않을 경우 {@code BusinessRuleViolationException} 예외가 발생합니다.
     * @param userId 사용자 ID
     * @return 해당 사용자의 포인트 잔액
     */
    public UserPointBalance getUserPointBalance(String userId){
        return userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.USER_NOT_FOUND, "해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));
    }

    /**
     * 특정 사용자의 포인트 내역 전체 조회.
     * <br></br>
     * 해당 사용자 존재하지 않을 경우 {@code BusinessRuleViolationException} 예외가 발생합니다.
     * @param userId
     * @return
     */
    public List<PointHistory> getUserPointHistories(String userId){
        List<PointHistory> pointHistories = pointHistoryRepository.findByUserId(userId);
        if(pointHistories.isEmpty()){throw new UnavailableRequestException(ErrorCode.USER_NOT_FOUND, "해당 회원이 존재하지 않으므로 포인트 내역 조회가 불가합니다.");}
        return pointHistories;
    }
}
