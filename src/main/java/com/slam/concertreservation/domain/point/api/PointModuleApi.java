package com.slam.concertreservation.domain.point.api;

import com.slam.concertreservation.interfaces.dto.UserPointBalanceResponse;

/**
 * 포인트 API 인터페이스
 */
public interface PointModuleApi {

    PointOperationResult decreaseUserPointBalance(Long userId, int amount);
    PointOperationResult increaseUserPointBalance(Long userId, int amount);
}
