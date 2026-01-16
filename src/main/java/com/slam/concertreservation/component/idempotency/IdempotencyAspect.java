package com.slam.concertreservation.component.idempotency;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyStorageService idempotencyStorageService;
    private final HttpServletRequest httpServletRequest; // HTTP 요청 헤더에서 Idempotency-Key 추출을 위함. 관련 문서 별도 참고.
    private final RedissonClient redissonClient;
    private final ResponseIdempotencyCodec codec;

    @Around("@annotation(idempotent)")
    public Object handle(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        // 1. idempotency key 추출
        String idempotencyKey = resolveIdempotencyKeyFromRequestHeader();

        // 2. Lock Key, Cache Key 생성
        String lockKey = generateLockKey(idempotencyKey, idempotent.operationKey());
        String cacheKey = generateCacheKey(idempotencyKey, idempotent.operationKey());

        // 3. Cache key 조회 : 기존에 처리된 기록이 있는지 확인
        Optional<IdempotencyRecord> existingRecordOpt = idempotencyStorageService.getIdempotencyRecord(cacheKey);

        // ** Case A : 캐시 존재 -> 캐시된 응답 반환 ** //
        if (existingRecordOpt.isPresent()) {
            log.debug("[IdempotencyAspect] 캐시 히트. Idempotency-Key: {}, OperationKey: {}", idempotencyKey, idempotent.operationKey());
            return codec.decode(existingRecordOpt.get());
        }

        // ** Case B : 캐시 미존재 -> Lock 획득 시도 후 신규 처리 로직 진행 ** //
        RLock lock = redissonClient.getLock(lockKey);

        // 4. Lock 획득 시도 (non-blocking tryLock)
        boolean lockAcquired = lock.tryLock();

        if (!lockAcquired) {
            // Case B-1 : Lock 획득 실패 -> 다른 요청이 처리 중
            log.info("[IdempotencyAspect] Lock 획득 실패. 다른 요청이 처리 중입니다. Idempotency-Key: {}, OperationKey: {}", idempotencyKey, idempotent.operationKey());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header(HttpHeaders.RETRY_AFTER, "5")
                    .build();
        }

        // Case B-2 : Lock 획득 성공 -> 비즈니스 로직 실행 및 캐싱
        try {
            // 5. 비즈니스 로직 실행
            Object result = pjp.proceed();

            // 6. ResponseEntity 인 경우에만 캐싱
            if (result instanceof ResponseEntity<?> response) {
                IdempotencyRecord record = codec.encode(response);
                idempotencyStorageService.storeIdempotencyRecord(cacheKey, record);
                log.debug("[IdempotencyAspect] 응답 캐싱 완료. Idempotency-Key: {}, OperationKey: {}", idempotencyKey, idempotent.operationKey());
            }

            return result;
        } finally {
            // 7. Lock 해제 (반드시 finally 에서 해제)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * HTTP Request Header 에서 Idempotency-Key 를 추출.
     * @return
     */
    private String resolveIdempotencyKeyFromRequestHeader(){
        String idemKey = httpServletRequest.getHeader("Idempotency-Key");
        if(idemKey == null || idemKey.isBlank()){
            throw new UnavailableRequestException(ErrorCode.INVALID_REQUEST, "Idempotency-Key 헤더가 존재하지 않습니다.");
        }
        return idemKey.trim();
    }

    /**
     * Lock Key 생성
     * @param idempotencyKey
     * @param operationKey
     * @return
     */
    private String generateLockKey(String idempotencyKey, String operationKey){
        return "IDEMPOTENCY:LOCK:" + operationKey + ":" + idempotencyKey;
    }

    /**
     * Cache Key 생성
     * @param idempotencyKey
     * @param operationKey
     * @return
     */
    private String generateCacheKey(String idempotencyKey, String operationKey){
        return "IDEMPOTENCY:RESULT:" + operationKey + ":" + idempotencyKey;
    }

}
