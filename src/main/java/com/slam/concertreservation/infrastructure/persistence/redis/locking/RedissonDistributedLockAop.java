package com.slam.concertreservation.infrastructure.persistence.redis.locking;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.exceptions.ConcurrencyException;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
@Slf4j
@RequiredArgsConstructor
public class RedissonDistributedLockAop {

    private static final String LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;

    @Around("@annotation(redissonDistributedLock)")
    public Object lock(ProceedingJoinPoint pjp, RedissonDistributedLock redissonDistributedLock) throws Throwable {

        // 1) 락을 걸 대상의 키를 추출
        String paramName = redissonDistributedLock.key();

        // 2) 메서드의 파라미터 이름과 값 추출
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] parameterNames = signature.getParameterNames();  // ex) ["concertId","seatId"]
        Object[] args = pjp.getArgs();                            // ex) ["C123","S45"]

        // 파라미터 이름으로 파라미터 인덱스 찾기
        int paramIndex = -1;
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                paramIndex = i;
                break;
            }
        }
        if (paramIndex == -1) {
            throw new BusinessRuleViolationException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "해당 이름을 가진 메서드 파라미터가 존재하지 않습니다. : " + paramName + " _ 발생 메서드 : " + signature.getMethod());
        }

        // 3) 락을 걸 대상의 키 생성. ex) LOCK:concertId-C123
        String lockKey = LOCK_PREFIX + ":" + parameterNames[paramIndex] + "-" + args[paramIndex];

        // Redisson의 ReentrantLock 구현체인 RLock 객체 생성
        RLock rLock = redissonClient.getFairLock(lockKey);
        log.info("[RedissonLockAspect] 다음 키에 대한 락 획득 시도 중입니다.: {}", lockKey);

        boolean currentlyLocked = false; // 현재 락 획득 상태

        try {
            boolean isLockAvailable = rLock.tryLock(redissonDistributedLock.waitTime(), redissonDistributedLock.leaseTime(), redissonDistributedLock.timeUnit());
            if (!isLockAvailable) {
                throw new ConcurrencyException(ErrorCode.INTERNAL_SERVER_ERROR, "다음 자원에 대한 락 획득에 실패하였습니다. 이미 해당 자원에 대한 락이 존재합니다.: " + lockKey);
            }
            rLock.lock(); // 락 실시
            currentlyLocked = true; // 현재 락 획득 상태 변경.
            log.info("[RedissonLockAspect] 다음 키에 대한 락 획득에 성공하였습니다.: {}", lockKey);
            return pjp.proceed(); // 해당 어노테이션이 표기된 메서드 실행. 이때 해당 메서드의 @Transactional 이 적용된다.

        } finally {
            // 메서드 실행 후 락 해제.
            if (currentlyLocked && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.info("[RedissonLockAspect] 다음 키에 대한 락이 해제되었습니다.: {}", lockKey);
            }
        }
    }
}