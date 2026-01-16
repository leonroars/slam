package com.slam.concertreservation.component.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResponseIdempotencyCodec {
    private final ObjectMapper objectMapper;

    public IdempotencyRecord encode(ResponseEntity<?> response) {
        Object body = response.getBody();
        String bodyType = body != null ? body.getClass().getName() : null;

        try {
            String bodyJson = body != null ? objectMapper.writeValueAsString(body) : null;
            return IdempotencyRecord.builder()
                    .httpStatusCode(response.getStatusCode().value())
                    .body(bodyJson)
                    .bodyType(bodyType)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("ResponseEntity<T> -> IdempotencyRecord 인코딩에 실패하였습니다.", e);
        }
    }

    public ResponseEntity<Object> decode(IdempotencyRecord record) {
        Object body = deserializeBody(record);
        return ResponseEntity.status(record.getHttpStatusCode()).body(body);
    }

    private Object deserializeBody(IdempotencyRecord record){
        if(record.getBody() == null || record.getBodyType() == null){
            return null;
        }
        try {
            Class<?> bodyClass = Class.forName(record.getBodyType());
            return objectMapper.readValue(record.getBody(), bodyClass);
        } catch (Exception e) {
            throw new RuntimeException("IdempotencyRecord -> ResponseEntity<T> 디코딩에 실패하였습니다.", e);
        }
    }
}
