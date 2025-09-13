package com.slam.concertreservation.domain.queue.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.queue.policy")
@Data
public class QueuePolicy {
    private int waitingTokenDuration; // 대기 상태 토큰의 유효시간 (분)
    private int activeTokenDuration; // 활성 상태 토큰의 유효시간 (분)
    private int maxConcurrentUser; // 최대 동시 예약 가능한 사용자 수
    private double maxConcurrentUserThreshold; // N 초에 M 명 활성화 시에도 지켜져야하는 최대 동시 사용자 산출 계수.

    public int calculateConcurrentUserThreshold(){
        return (int)Math.floor(maxConcurrentUser * maxConcurrentUserThreshold);
    }
}
