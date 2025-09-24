package com.slam.concertreservation.component.validator.token;

import com.slam.concertreservation.domain.queue.service.QueueService;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TokenValidationAspect {

    private final QueueService queueService;

    @Pointcut("@annotation(com.slam.concertreservation.component.validator.token.RequiresTokenValidation)")
    public void tokenValidationPointcut() {}

    @Before("tokenValidationPointcut() && args(concertScheduleId, tokenId,..)")
    public void validateToken(JoinPoint joinPoint, String concertScheduleId, String tokenId) {

        boolean isValid = queueService.validateToken(concertScheduleId, tokenId);
        if (!isValid) {
            throw new UnavailableRequestException("해당 토큰은 유효하지 않습니다.");
        }
    }
}

