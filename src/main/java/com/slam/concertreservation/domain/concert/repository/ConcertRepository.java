package com.slam.concertreservation.domain.concert.repository;

import com.slam.concertreservation.domain.concert.model.Concert;
import java.util.Optional;

public interface ConcertRepository {

    Concert save(Concert concert);

    Optional<Concert> findById(Long id);
}
