package com.slam.concertreservation.domain.point.repository;

import com.slam.concertreservation.domain.point.model.UserPointBalance;
import java.util.Optional;

public interface UserPointBalanceRepository {

    /*
     * 사용자 포인트 잔액 조회.
     * 현재 구현 상 '새로운 사용자 생성(가입) 시엔 잔액 0을 갖는 UserPointBalance 를 생성하여 저장하도록 하고있습니다.
     * 따라서 Optional<UserPointBalance> == null 인 경우는 오직 '해당 사용자가 존재하지 않는 경우' 입니다.
     */
    Optional<UserPointBalance> getBalanceByUserId(Long userId);

    // 사용자 포인트 잔액 저장
    UserPointBalance save(UserPointBalance userPointBalance);
}
