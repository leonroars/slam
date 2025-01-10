package com.hhp7.concertreservation.domain.point.service;

import com.hhp7.concertreservation.domain.point.model.Point;
import com.hhp7.concertreservation.domain.point.model.PointHistory;
import com.hhp7.concertreservation.domain.point.model.PointTransactionType;
import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import com.hhp7.concertreservation.domain.point.repository.PointHistoryRepository;
import com.hhp7.concertreservation.domain.point.repository.UserPointBalanceRepository;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointBalanceRepository userPointBalanceRepository;

    /**
     * 특정 사용자의 포인트 잔액을 감액한 후 변동된 잔액을 저장하고 이를 반환한다.
     * <br></br>
     * 또한, 이에 대한 내역을 생성하여 저장한다.
     *
     * @param userId 사용자 ID
     * @param decreaseAmount 감액량
     * @return 변동된 사용자의 잔액
     */
    public UserPointBalance decreaseUserPointBalance(String userId, int decreaseAmount){
        UserPointBalance userPointBalance = userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException("해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));;
        userPointBalance.decrease(decreaseAmount);
        PointHistory pointHistory = PointHistory.create(
                userId,
                PointTransactionType.USE,
                decreaseAmount
        );
        pointHistoryRepository.save(pointHistory);
        return userPointBalanceRepository.save(userPointBalance);
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
    public UserPointBalance increaseUserPointBalance(String userId, int increaseAmount){
        UserPointBalance userPointBalance = userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException("해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));
        userPointBalance.increase(increaseAmount);
        PointHistory pointHistory = PointHistory.create(
                userId,
                PointTransactionType.CHARGE,
                increaseAmount
        );
        pointHistoryRepository.save(pointHistory);
        return userPointBalanceRepository.save(userPointBalance);

    }

    /**
     * 신규 사용자의 포인트 잔액과 이에 대한 내역을 생성 및 저장한다.
     * <br></br>
     * 잔액 0인 UserPointBalance를 반환한다.
     *
     * @param userId 사용자 ID
     * @return 잔액 0인 사용자 잔액
     * */
    public UserPointBalance createUserPointBalance(String userId){
        UserPointBalance userPointBalance = UserPointBalance.create(userId, Point.create(0));
        pointHistoryRepository.save(PointHistory.create(userId, PointTransactionType.INIT, 0));
        return userPointBalanceRepository.save(userPointBalance);
    }

    /**
     * 특정 사용자의 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 포인트 잔액
     */
    public UserPointBalance getUserPointBalance(String userId){
        return userPointBalanceRepository.getBalanceByUserId(userId)
                .orElseThrow(() -> new UnavailableRequestException("해당 회원이 존재하지 않으므로 잔액 조회가 불가합니다."));
    }
}
