package com.hhp7.concertreservation.domain.point.repository;

import com.hhp7.concertreservation.domain.point.model.PointHistory;
import java.util.List;
import java.util.Optional;

public interface PointHistoryRepository {

    // 포인트 내역 저장
    PointHistory save(PointHistory pointHistory);

    // 사용자 포인트 내역 가져오기
    List<PointHistory> findByUserId(String userId);
}
