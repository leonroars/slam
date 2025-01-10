package com.hhp7.concertreservation.domain.point.repository;

import com.hhp7.concertreservation.domain.point.model.UserPointBalance;
import java.util.Optional;

public interface UserPointBalanceRepository {

    // 사용자 포인트 잔액 조회
    Optional<UserPointBalance> getBalanceByUserId(String userId);

    // 사용자 포인트 잔액 저장
    UserPointBalance save(UserPointBalance userPointBalance);
}
