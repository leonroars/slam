package com.slam.concertreservation.domain.point.repository;

import com.slam.concertreservation.domain.point.model.PointHistory;
import java.util.List;

public interface PointHistoryRepository {

    // 포인트 내역 저장
    PointHistory save(PointHistory pointHistory);

    // 특정 사용자의 전체 포인트 내역 가져오기
    List<PointHistory> findByUserId(Long userId);
}
