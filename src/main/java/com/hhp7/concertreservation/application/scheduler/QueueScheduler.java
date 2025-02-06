package com.hhp7.concertreservation.application.scheduler;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import com.hhp7.concertreservation.domain.concert.service.ConcertService;
import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.service.QueueService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueService queueService;
    private final ConcertService concertService;

    private static final int ACTIVATION_BATCH_SIZE = 10;

    // Queue 내에 저장된 Token을 만료시키고 만료시킨 토큰 수만큼 활성화 시켜준다.
    @Scheduled(fixedDelay = 10000) // 10초 간격 순회하며 작업.
    public void expireAndActivateToken(String concertScheduleId) {

        // 현재 예약 진행 중인 공연 전체 조회
        List<ConcertSchedule> onGoingConcertSchedules = concertService.getOngoingConcertSchedules(LocalDateTime.now());

        for(ConcertSchedule concertSchedule : onGoingConcertSchedules){
            // 만료 대상 활성화 토큰 조회 -> 서비스로부터 쫓겨나야하는 사람들 추리기!
            List<Token> activeTokensToBeExpired = queueService.getActivatedTokensToBeExpired(concertSchedule.getId());

            // 만료 대상 토큰이 존재할 경우 -> 만료 시키기.
            if(!activeTokensToBeExpired.isEmpty()){
                // 만료 대상 토큰 만료 처리.
                queueService.expireToken(concertScheduleId, activeTokensToBeExpired);
                // 정책 상 설정된 활성화 갯수만큼 토큰 활성화.
                queueService.activateTokens(concertScheduleId, ACTIVATION_BATCH_SIZE);
            }
        }
    }

    // 만료된 토큰 삭제하기 : 해당 공연 일정에 대한 예약 종료 후(당일 자정 00:00 시 이후)에 해당 토큰을 삭제한다.
}
