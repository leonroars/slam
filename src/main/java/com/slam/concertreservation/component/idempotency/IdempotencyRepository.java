package com.slam.concertreservation.component.idempotency;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final RedisTemplate<String, IdempotencyRecord> idempotencyRecordRedisTemplate;

    /**
     * idempotencyKey 에 해당하는 IdempotencyRecord 를 Redis 에 저장합니다.
     * @param idempotencyKey
     * @param record
     * @param ttl
     */
    public void save(String idempotencyKey, IdempotencyRecord record, Duration ttl) {
        idempotencyRecordRedisTemplate.opsForValue()
                .set(idempotencyKey, record, ttl);
    }

    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        IdempotencyRecord record = idempotencyRecordRedisTemplate.opsForValue()
                .get(idempotencyKey);
        return Optional.ofNullable(record);
    }

    /**
     * idempotencyKey 가 존재하지 않을 때에만 IdempotencyRecord 를 Redis 에 저장합니다.
     * <br></br>
     * 목적 : 복수의 요청이 동시에 혹은 거의 동시에 애플리케이션에 진입했을 때, 최초 요청에 대해서만 처리하고 이후 요청에 대해서는 무시하여 경합 수준을 낮추기 위함.
     * <br></br>
     * 작동 방식 : Redis 의 SET...NX EX {ttl} 과 동일하게 작동.
     * <br></br>
     * @param idempotencyKey
     * @param record
     * @param ttl
     * @return
     */
    public boolean saveIfAbsent(String idempotencyKey, IdempotencyRecord record, Duration ttl) {
        Boolean success = idempotencyRecordRedisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, record, ttl);
        return Boolean.TRUE.equals(success);
    }
}
