package com.hhp7.concertreservation.application.scheduler;

import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.service.QueueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueService queueService;

    // Queue 내에 저장된 Token을 만료시키고 만료시킨 토큰 수만큼 활성화 시켜준다.
    @Scheduled(fixedDelay = 10000) // 약 10초 간격 순회하며 작업.
    public void expireAndActivateToken() {
        // 만료 대상 토큰 조회.
        List<Token> expiredTokens = queueService.getExpiredTokens();
        String concertScheduleId = expiredTokens.get(0).getConcertScheduleId();

        // 만료 대상 토큰 만료 처리.
        queueService.expireToken(expiredTokens);

        // 토큰 활성화
        queueService.activateTokens(concertScheduleId);
    }
}
