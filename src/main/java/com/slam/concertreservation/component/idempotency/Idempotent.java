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
     * 메서드 매개변수나 기타 컨텍스트에서 Idempotency Key 를 동적으로 생성하는 데 사용됩니다.
     */
    String key();

    /**
     * 응답 캐시 지속 시간 단위 및 지속 시간 정의
     * @return
     */
    TimeUnit responseCacheTimeUnit() default TimeUnit.HOURS;
    long responseCacheDuration() default 24;

}
