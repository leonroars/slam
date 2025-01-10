package com.hhp7.concertreservation.domain.concert.repository;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import java.util.Optional;


public interface ConcertRepository {

    // 콘서트 등록
    Concert save(Concert concert);

    // 콘서트 조회
    Optional<Concert> findById(String concertId);
}
