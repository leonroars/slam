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

    /**
     * idempotencyKey 에 해당하는 IdempotencyRecord 를 Redis 에서 조회합니다.
     * @param idempotencyKey
     * @return
     */
    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        IdempotencyRecord record = idempotencyRecordRedisTemplate.opsForValue()
                .get(idempotencyKey);
        return Optional.ofNullable(record);
    }
}
