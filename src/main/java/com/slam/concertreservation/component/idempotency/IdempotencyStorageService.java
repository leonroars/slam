package com.slam.concertreservation.component.idempotency;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyStorageService {

    private final IdempotencyRepository idempotencyRepository;
    private static final Duration IDEMPOTENCY_RECORD_TTL = Duration.ofHours(12);

    /**
     * idempotencyKey 에 해당하는 IdempotencyRecord 를 저장합니다.
     * @param idempotencyKey
     * @param record
     */
    public void storeIdempotencyRecord(String idempotencyKey, IdempotencyRecord record) {
        idempotencyRepository.save(idempotencyKey, record, IDEMPOTENCY_RECORD_TTL);
    }

    /**
     * idempotencyKey 에 해당하는 IdempotencyRecord 를 조회합니다.\
     * <br></br>
     * 존재하지 않을 시 빈 Optional 을 반환합니다. -> 추후 Aspect 단에서 존재 유무에 따른 분기 처리 가능하도록 하기 위함.
     * @param idempotencyKey
     * @return
     */
    public Optional<IdempotencyRecord> getIdempotencyRecord(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey);
    }

}
