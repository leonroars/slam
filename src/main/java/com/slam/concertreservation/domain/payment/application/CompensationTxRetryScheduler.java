package com.slam.concertreservation.domain.payment.application;

import com.slam.concertreservation.domain.payment.model.CompensationTxLog;
import com.slam.concertreservation.domain.payment.service.CompensationTxLogService;
import com.slam.concertreservation.domain.point.api.PointModuleApi;
import com.slam.concertreservation.domain.point.api.PointOperationResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationTxRetryScheduler {

    private final CompensationTxLogService compensationTxLogService;
    private final PointModuleApi pointModuleApi;

    @Scheduled(fixedDelay = 60000)  // 1분 간격
    public void retryFailedCompensations() {
        List<CompensationTxLog> targets = compensationTxLogService.getAllRetriables();

        for (CompensationTxLog log : targets) {
            PointOperationResult result;

            // 양수: 포인트 복구(증가), 음수: 포인트 회수(차감)
            if (log.getPrice() > 0) {
                result = pointModuleApi.increaseUserPointBalance(log.getUserId(), log.getPrice());
            } else {
                result = pointModuleApi.decreaseUserPointBalance(log.getUserId(), Math.abs(log.getPrice()));
            }

            if (result.success()) {
                compensationTxLogService.markAsCompleted(log);
            } else {
                compensationTxLogService.markAsFailed(log);
            }
        }
    }
}
