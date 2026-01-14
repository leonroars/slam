package com.slam.concertreservation.component.idempotency;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Builder
@Data
public class IdempotencyRecord {

    HttpStatusCode httpStatusCode; // 캐시된 응답의 HTTP 응답 코드
    Map<String, String> headers; // 캐시된 응답의 HTTP 헤더
    String body; // ResponseEntity 의 Body 를 구성하는 DTO 의 직렬화된 형태
    String bodyType; // ObjectMapper 를 활용해 Body 역직렬화 시 참고할 클래스 식별자
    IdempotencyRecordStatus status; // PROCESSING, COMPLETED

    public void processing() {this.status = IdempotencyRecordStatus.PROCESSING;}

    public void completed() {this.status = IdempotencyRecordStatus.COMPLETED;}

    public boolean isProcessing() {return this.status == IdempotencyRecordStatus.PROCESSING;}

    public boolean isCompleted() {return this.status == IdempotencyRecordStatus.COMPLETED;}
}
