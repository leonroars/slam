package com.slam.concertreservation.component.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Builder
@Data
// Jackson 직렬화/역직렬화를 위해 기본 생성자 필요
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    int httpStatusCode; // 캐시된 응답의 HTTP 응답 코드(HttpStatusCode 는 Interface 이기 때문에 구체 클래스 정보가 없어 직렬화/역직렬화에 어려움이 있음. 따라서 int 타입으로 저장)
    String body; // ResponseEntity 의 Body 를 구성하는 DTO 의 직렬화된 형태
    String bodyType; // 직렬화된 Body 의 타입 정보를 FQCN 으로 저장(Fully Qualified Class Name)
    IdempotencyRecordStatus status; // PROCESSING, COMPLETED
    Instant createdAt; // 레코드 생성 시각

    public void processing() {this.status = IdempotencyRecordStatus.PROCESSING;}

    public void completed() {this.status = IdempotencyRecordStatus.COMPLETED;}

    public boolean isProcessing() {return this.status == IdempotencyRecordStatus.PROCESSING;}

    public boolean isCompleted() {return this.status == IdempotencyRecordStatus.COMPLETED;}

    public HttpStatusCode getHttpStatusCode() {
        return HttpStatusCode.valueOf(this.httpStatusCode);
    }

}
