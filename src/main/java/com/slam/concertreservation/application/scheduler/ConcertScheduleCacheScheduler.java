package com.slam.concertreservation.application.scheduler;

import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.service.ConcertService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertScheduleCacheScheduler {

    private final ConcertService concertService;

    // 공연 일정 캐시 스케줄러
    @CachePut(value = "ongoingConcertSchedules", key = "'list'")
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 캐시를 갱신.
    public List<ConcertSchedule> refreshOngoingConcertSchedules() {
        return concertService.getOngoingConcertSchedules(LocalDateTime.now());
    }

}
