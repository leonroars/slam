package com.slam.concertreservation.component.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotencyAspectUnitTest {

    @Mock
    private IdempotencyStorageService idempotencyStorageService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ResponseIdempotencyCodec codec;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private RLock lock;

    private IdempotencyAspect aspect;

    // 테스트용 DTO
    record SampleDto(String id, String name) {}

    // 테스트용 메서드 (어노테이션 추출용)
    @Idempotent(operationKey = "test.operation")
    public void testMethod() {}

    @Idempotent(operationKey = "reservation.create")
    public void reservationCreateMethod() {}

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        MockitoAnnotations.openMocks(this);
        aspect = new IdempotencyAspect(idempotencyStorageService, httpServletRequest, redissonClient, codec);

        // 공통 Mock 설정 - MethodSignature를 통해 어노테이션 추출
        Method testMethod = this.getClass().getMethod("testMethod");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod);
    }

    private void setupMethodSignature(String methodName) throws NoSuchMethodException {
        Method method = this.getClass().getMethod(methodName);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    @Nested
    @DisplayName("Idempotency-Key 헤더 검증 테스트")
    class IdempotencyKeyValidationTest {

        @Test
        @DisplayName("실패 : Idempotency-Key 헤더가 없으면 UnavailableRequestException이 발생한다")
        void shouldThrowExceptionWhenIdempotencyKeyHeaderIsMissing() {
            // given
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> aspect.handle(pjp))
                    .isInstanceOf(UnavailableRequestException.class)
                    .hasMessageContaining("Idempotency-Key");
        }

        @Test
        @DisplayName("실패 : Idempotency-Key 헤더가 빈 문자열이면 UnavailableRequestException이 발생한다")
        void shouldThrowExceptionWhenIdempotencyKeyHeaderIsBlank() {
            // given
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("   ");

            // when & then
            assertThatThrownBy(() -> aspect.handle(pjp))
                    .isInstanceOf(UnavailableRequestException.class)
                    .hasMessageContaining("Idempotency-Key");
        }

        @Test
        @DisplayName("성공 : Idempotency-Key 헤더 값의 앞뒤 공백이 제거된다")
        void shouldTrimIdempotencyKeyHeader() throws Throwable {
            // given
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("  test-key-123  ");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            when(pjp.proceed()).thenReturn(ResponseEntity.ok("result"));
            when(codec.encode(any())).thenReturn(IdempotencyRecord.builder().build());

            // when
            aspect.handle(pjp);

            // then - cache key에 trimmed 값이 사용되었는지 확인
            verify(idempotencyStorageService).getIdempotencyRecord("IDEMPOTENCY:RESULT:test.operation:test-key-123");
        }
    }

    @Nested
    @DisplayName("Case A: 캐시 히트 테스트")
    class CacheHitTest {

        @Test
        @DisplayName("성공 : 캐시가 존재하면 캐시된 응답을 반환하고 비즈니스 로직을 실행하지 않는다")
        void shouldReturnCachedResponseWhenCacheExists() throws Throwable {
            // given
            String idempotencyKey = "test-key-123";
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn(idempotencyKey);

            IdempotencyRecord cachedRecord = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body("{\"id\":\"1\",\"name\":\"cached\"}")
                    .bodyType(SampleDto.class.getName())
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.of(cachedRecord));

            ResponseEntity<SampleDto> decodedResponse = ResponseEntity.ok(new SampleDto("1", "cached"));
            doReturn(decodedResponse).when(codec).decode(cachedRecord);

            // when
            Object result = aspect.handle(pjp);

            // then
            assertThat(result).isEqualTo(decodedResponse);
            verify(pjp, never()).proceed();
            verify(redissonClient, never()).getLock(anyString());
            verify(idempotencyStorageService, never()).storeIdempotencyRecord(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Case B-1: Lock 획득 실패 테스트")
    class LockFailureTest {

        @BeforeEach
        void setUpCacheMiss() {
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("new-key-456");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
        }

        @Test
        @DisplayName("성공 : Lock 획득 실패 시 202 응답과 Retry-After 헤더를 반환한다")
        void shouldReturn202WithRetryAfterWhenLockAcquisitionFails() throws Throwable {
            // given
            when(lock.tryLock()).thenReturn(false);

            // when
            Object result = aspect.handle(pjp);

            // then
            assertThat(result).isInstanceOf(ResponseEntity.class);
            ResponseEntity<?> response = (ResponseEntity<?>) result;

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
            assertThat(response.getBody()).isNull();

            verify(pjp, never()).proceed();
            verify(codec, never()).encode(any());
            verify(idempotencyStorageService, never()).storeIdempotencyRecord(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Case B-2: Lock 획득 성공 테스트")
    class LockSuccessTest {

        @BeforeEach
        void setUpCacheMissAndLock() {
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("new-key-789");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
        }

        @Test
        @DisplayName("성공 : Lock 획득 성공 시 비즈니스 로직을 실행하고 결과를 캐싱한다")
        void shouldExecuteAndCacheWhenLockAcquired() throws Throwable {
            // given
            ResponseEntity<SampleDto> businessResponse = ResponseEntity.ok(new SampleDto("1", "new"));
            when(pjp.proceed()).thenReturn(businessResponse);

            IdempotencyRecord encodedRecord = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body("{\"id\":\"1\",\"name\":\"new\"}")
                    .bodyType(SampleDto.class.getName())
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();
            when(codec.encode(businessResponse)).thenReturn(encodedRecord);

            // when
            Object result = aspect.handle(pjp);

            // then
            assertThat(result).isEqualTo(businessResponse);
            verify(pjp).proceed();
            verify(codec).encode(businessResponse);
            verify(idempotencyStorageService).storeIdempotencyRecord(anyString(), eq(encodedRecord));
            verify(lock).unlock();
        }

        @Test
        @DisplayName("성공 : 비즈니스 로직 실행 중 예외 발생 시에도 Lock을 해제한다")
        void shouldUnlockEvenWhenBusinessLogicThrowsException() throws Throwable {
            // given
            when(pjp.proceed()).thenThrow(new RuntimeException("Business logic failed"));

            // when & then
            assertThatThrownBy(() -> aspect.handle(pjp))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Business logic failed");

            verify(lock).unlock();
            verify(idempotencyStorageService, never()).storeIdempotencyRecord(anyString(), any());
        }

        @Test
        @DisplayName("성공 : ResponseEntity가 아닌 결과는 캐싱하지 않는다")
        void shouldNotCacheNonResponseEntityResult() throws Throwable {
            // given
            when(pjp.proceed()).thenReturn("plain string result");

            // when
            Object result = aspect.handle(pjp);

            // then
            assertThat(result).isEqualTo("plain string result");
            verify(codec, never()).encode(any());
            verify(idempotencyStorageService, never()).storeIdempotencyRecord(anyString(), any());
            verify(lock).unlock();
        }

        @Test
        @DisplayName("성공 : null 결과는 캐싱하지 않는다")
        void shouldNotCacheNullResult() throws Throwable {
            // given
            when(pjp.proceed()).thenReturn(null);

            // when
            Object result = aspect.handle(pjp);

            // then
            assertThat(result).isNull();
            verify(codec, never()).encode(any());
            verify(idempotencyStorageService, never()).storeIdempotencyRecord(anyString(), any());
            verify(lock).unlock();
        }
    }

    @Nested
    @DisplayName("Key 생성 로직 테스트")
    class KeyGenerationTest {

        @Test
        @DisplayName("성공 : Lock Key는 'IDEMPOTENCY:LOCK:{operationKey}:{idempotencyKey}' 형식으로 생성된다")
        void shouldGenerateLockKeyWithCorrectFormat() throws Throwable {
            // given
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("my-key");
            setupMethodSignature("reservationCreateMethod");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(false);

            // when
            aspect.handle(pjp);

            // then
            verify(redissonClient).getLock("IDEMPOTENCY:LOCK:reservation.create:my-key");
        }

        @Test
        @DisplayName("성공 : Cache Key는 'IDEMPOTENCY:RESULT:{operationKey}:{idempotencyKey}' 형식으로 생성된다")
        void shouldGenerateCacheKeyWithCorrectFormat() throws Throwable {
            // given
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("my-key");
            setupMethodSignature("reservationCreateMethod");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(false);

            // when
            aspect.handle(pjp);

            // then
            verify(idempotencyStorageService).getIdempotencyRecord("IDEMPOTENCY:RESULT:reservation.create:my-key");
        }
    }

    @Nested
    @DisplayName("Lock 해제 안전성 테스트")
    class LockSafetyTest {

        @BeforeEach
        void setUpCacheMissAndLock() {
            when(httpServletRequest.getHeader("Idempotency-Key")).thenReturn("test-key");
            when(idempotencyStorageService.getIdempotencyRecord(anyString()))
                    .thenReturn(Optional.empty());
            when(redissonClient.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock()).thenReturn(true);
        }

        @Test
        @DisplayName("성공 : 현재 스레드가 Lock을 보유하지 않으면 unlock을 호출하지 않는다")
        void shouldNotUnlockWhenNotHeldByCurrentThread() throws Throwable {
            // given
            when(lock.isHeldByCurrentThread()).thenReturn(false);
            when(pjp.proceed()).thenReturn(ResponseEntity.ok("result"));
            when(codec.encode(any())).thenReturn(IdempotencyRecord.builder().build());

            // when
            aspect.handle(pjp);

            // then
            verify(lock, never()).unlock();
        }

        @Test
        @DisplayName("성공 : 현재 스레드가 Lock을 보유하면 unlock을 호출한다")
        void shouldUnlockWhenHeldByCurrentThread() throws Throwable {
            // given
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            when(pjp.proceed()).thenReturn(ResponseEntity.ok("result"));
            when(codec.encode(any())).thenReturn(IdempotencyRecord.builder().build());

            // when
            aspect.handle(pjp);

            // then
            verify(lock).unlock();
        }
    }
}
