package com.slam.concertreservation.component.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 어노테이션을 사용하고자 하는 API Endpoint 가 표현하는 자원 및 행위 식별자
     * <br></br>
     * ex. reservation.create, payment.request
     * <br></br>
     * 이후 Redis Key 생성 시 활용됨. (ex. IDEMPOTENCY:RESULT:{operationKey}:{idempotencyKey})
     */
    String operationKey();

    /**
     * 응답 캐시 지속 시간 단위 및 지속 시간 정의
     * @return
     */
    TimeUnit responseCacheTimeUnit() default TimeUnit.HOURS;
    long responseCacheDuration() default 24;

}
