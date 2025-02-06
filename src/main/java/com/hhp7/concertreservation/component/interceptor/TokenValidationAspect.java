package com.hhp7.concertreservation.component.interceptor;

import com.hhp7.concertreservation.domain.queue.service.QueueService;
import com.hhp7.concertreservation.exceptions.UnavailableRequestException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

@Aspect
@Component
@RequiredArgsConstructor
public class TokenValidationAspect {

    private final QueueService queueService;

    @Pointcut("@annotation(com.hhp7.concertreservation.component.interceptor.RequiresTokenValidation)")
    public void tokenValidationPointcut() {}

    @Before("tokenValidationPointcut() && args(concertScheduleId, tokenId,..)")
    public void validateToken(JoinPoint joinPoint, String concertScheduleId, String tokenId) {

        boolean isValid = queueService.validateToken(concertScheduleId, tokenId);
        if (!isValid) {
            throw new UnavailableRequestException("해당 토큰은 유효하지 않습니다.");
        }
    }
}

