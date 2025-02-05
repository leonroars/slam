package com.hhp7.concertreservation.infrastructure.redis.redisson.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redisson에서 제공하는 분산 락 활용을 위한 AOP 어노테이션.
 */
@Target(ElementType.METHOD) // 메소드에 적용 가능하도록 설정
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 어노테이션 정보 유지
public @interface RedissonDistributedLock {

    /**
     * 락의 대상이 되는 자원의 식별자. ex. userId, concertScheduleId 등
     */
    String key();

    /**
     * 락이 유지되는 시간 : 별도의 시간 단위 명시 없을 경우 초 단위로 설정.
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 락 획득을 기다리는 시간 (default - 5s). 즉, 락 획득 실패 시 재시도까지 기다리는 시간을 의미.
     */
    long waitTime() default 10L;

    /**
     * 락 임대 시간 (default - 3s)
     * 락을 획득한 이후 leaseTime 이 지나면 락을 해제한다
     */
    long leaseTime() default 3L;
}