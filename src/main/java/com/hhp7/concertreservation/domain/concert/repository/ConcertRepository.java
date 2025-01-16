package com.hhp7.concertreservation.domain.concert.repository;

import com.hhp7.concertreservation.domain.concert.model.Concert;
import java.util.Optional;

public interface ConcertRepository {

    Concert save(Concert concert);

    Optional<Concert> findById(String id);
}
