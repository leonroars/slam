package com.slam.concertreservation.component.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IdempotencyIntegrationTest.TestIdempotencyController.class)
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 테스트용 DTO
    record TestRequest(String name, int value) {}
    record TestResponse(String id, String name, int value) {}

    @BeforeEach
    void cleanUp() {
        // 테스트 간 격리를 위해 IDEMPOTENCY 관련 키 삭제
        var keys = redisTemplate.keys("IDEMPOTENCY:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 테스트용 컨트롤러
     *
     * 핵심 포인트:
     * - slowOperation()에 latch 훅을 넣어 "락을 잡은 상태"를 sleep 없이 확정할 수 있도록 함
     * - 이 훅은 동시성 테스트에서만 사용되며, 그 외 테스트는 기존 지연(sleep) 로직을 그대로 유지
     */
    @RestController
    @RequestMapping("/test/idempotency")
    static class TestIdempotencyController {

        private final AtomicInteger executionCount = new AtomicInteger(0);

        // ====== 테스트 훅 (동시성 테스트 안정화 용도) ======
        private volatile CountDownLatch slowEnteredLatch;
        private volatile CountDownLatch slowReleaseLatch;

        public void initSlowHooks(CountDownLatch entered, CountDownLatch release) {
            this.slowEnteredLatch = entered;
            this.slowReleaseLatch = release;
        }

        public void clearSlowHooks() {
            this.slowEnteredLatch = null;
            this.slowReleaseLatch = null;
        }

        @PostMapping("/create")
        @Idempotent(operationKey = "test.create")
        public ResponseEntity<TestResponse> create(@RequestBody TestRequest request) throws InterruptedException {
            executionCount.incrementAndGet();
            // 비즈니스 로직 시뮬레이션 (약간의 지연)
            Thread.sleep(100);
            TestResponse response = new TestResponse(
                    UUID.randomUUID().toString(),
                    request.name(),
                    request.value()
            );
            return ResponseEntity.ok(response);
        }

        @PostMapping("/slow")
        @Idempotent(operationKey = "test.slow")
        public ResponseEntity<TestResponse> slowOperation(@RequestBody TestRequest request) throws InterruptedException {
            executionCount.incrementAndGet();

            // ✅ 컨트롤러 진입 = AOP 락 획득 완료 시점으로 간주 가능 (락은 보통 handler 호출 전 획득)
            if (slowEnteredLatch != null) {
                slowEnteredLatch.countDown();
            }

            // ✅ 락을 오래 유지하여, 다른 요청이 반드시 202(락 경합 실패)를 받도록 보장
            if (slowReleaseLatch != null) {
                slowReleaseLatch.await(3, TimeUnit.SECONDS); // 무한정 블로킹 방지용 timeout
            } else {
                // 느린 비즈니스 로직 시뮬레이션(기본 동작)
                Thread.sleep(500);
            }

            TestResponse response = new TestResponse(
                    UUID.randomUUID().toString(),
                    request.name(),
                    request.value()
            );
            return ResponseEntity.ok(response);
        }

        public int getExecutionCount() {
            return executionCount.get();
        }

        public void resetExecutionCount() {
            executionCount.set(0);
        }
    }

    @Autowired
    private TestIdempotencyController testController;

    @Nested
    @DisplayName("기본 멱등성 테스트")
    class BasicIdempotencyTest {

        @Test
        @DisplayName("성공 : 첫 번째 요청은 비즈니스 로직을 실행하고 결과를 캐싱한다")
        void shouldExecuteAndCacheOnFirstRequest() throws Exception {
            // given
            String idempotencyKey = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("test", 100);
            testController.resetExecutionCount();

            // when
            mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("test"))
                    .andExpect(jsonPath("$.value").value(100))
                    .andReturn();

            // then
            assertThat(testController.getExecutionCount()).isEqualTo(1);

            // Redis에 캐시가 저장되었는지 확인 (키 포맷이 구현 디테일에 의존함에 유의)
            String cacheKey = "IDEMPOTENCY:RESULT:test.create:" + idempotencyKey;
            assertThat(redisTemplate.hasKey(cacheKey)).isTrue();
        }

        @Test
        @DisplayName("성공 : 동일한 Idempotency-Key로 두 번째 요청 시 캐시된 결과를 반환하고 비즈니스 로직을 실행하지 않는다")
        void shouldReturnCachedResultOnSecondRequest() throws Exception {
            // given
            String idempotencyKey = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("test", 100);
            testController.resetExecutionCount();

            // 첫 번째 요청
            MvcResult firstResult = mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String firstResponseId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                    .get("id").asText();

            // when - 두 번째 요청 (동일한 Idempotency-Key)
            MvcResult secondResult = mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String secondResponseId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                    .get("id").asText();

            // then
            assertThat(testController.getExecutionCount()).isEqualTo(1); // 비즈니스 로직은 1번만 실행
            assertThat(secondResponseId).isEqualTo(firstResponseId);     // 동일한 응답 반환
        }

        @Test
        @DisplayName("성공 : 다른 Idempotency-Key로 요청 시 각각 독립적으로 처리된다")
        void shouldProcessIndependentlyWithDifferentIdempotencyKeys() throws Exception {
            // given
            String idempotencyKey1 = UUID.randomUUID().toString();
            String idempotencyKey2 = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("test", 100);
            testController.resetExecutionCount();

            // when
            MvcResult result1 = mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey2)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String id1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .get("id").asText();
            String id2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .get("id").asText();

            // then
            assertThat(testController.getExecutionCount()).isEqualTo(2); // 각각 1번씩 실행
            assertThat(id1).isNotEqualTo(id2); // 다른 결과
        }
    }

    @Nested
    @DisplayName("Idempotency-Key 헤더 검증 테스트")
    class HeaderValidationTest {

        @Test
        @DisplayName("실패 : Idempotency-Key 헤더가 없으면 400 에러를 반환한다")
        void shouldReturn400WhenIdempotencyKeyHeaderIsMissing() throws Exception {
            // given
            TestRequest request = new TestRequest("test", 100);

            // when & then
            mockMvc.perform(post("/test/idempotency/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 : Idempotency-Key 헤더가 빈 문자열이면 400 에러를 반환한다")
        void shouldReturn400WhenIdempotencyKeyHeaderIsBlank() throws Exception {
            // given
            TestRequest request = new TestRequest("test", 100);

            // when & then
            mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", "   ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("동시성 테스트 (안정형: sleep 기반 타이밍 제거)")
    class ConcurrencyTest {

        @Test
        @DisplayName("성공 : 동일한 Idempotency-Key로 동시 요청 시 하나만 200, 나머지는 202를 반환한다 (완전 안정형)")
        void shouldReturn202ForConcurrentRequestsWithSameKey_stable() throws Exception {
            // given
            String idempotencyKey = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("concurrent", 100);
            testController.resetExecutionCount();

            int concurrentRequests = 5;

            CountDownLatch slowEntered = new CountDownLatch(1); // 첫 요청이 slowOperation에 진입했음을 보장
            CountDownLatch slowRelease = new CountDownLatch(1); // 첫 요청을 언제 끝낼지 테스트가 제어
            testController.initSlowHooks(slowEntered, slowRelease);

            ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

            try {
                // 1) 첫 번째 요청을 별도 스레드로 시작 (락 획득 + 메서드 진입 유도)
                Future<Integer> firstFuture = executor.submit(() -> {
                    MvcResult result = mockMvc.perform(post("/test/idempotency/slow")
                                    .header("Idempotency-Key", idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();
                    return result.getResponse().getStatus();
                });

                // 2) 첫 번째 요청이 메서드에 진입할 때까지 대기 (즉, 락을 잡고 있는 상태)
                assertThat(slowEntered.await(2, TimeUnit.SECONDS))
                        .as("첫 요청이 slowOperation에 진입하지 못함 (락 획득/진입 확인 불가)")
                        .isTrue();

                // 3) 나머지 요청들은 이 시점에 락 경합 상태 → 202가 안정적으로 나와야 함
                List<Callable<Integer>> tasks = new ArrayList<>();
                for (int i = 0; i < concurrentRequests - 1; i++) {
                    tasks.add(() -> {
                        MvcResult result = mockMvc.perform(post("/test/idempotency/slow")
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andReturn();
                        return result.getResponse().getStatus();
                    });
                }

                List<Future<Integer>> futures = executor.invokeAll(tasks);

                int acceptedCount = 0;
                int okCount = 0;

                for (Future<Integer> f : futures) {
                    int st = f.get(2, TimeUnit.SECONDS);
                    if (st == 202) acceptedCount++;
                    if (st == 200) okCount++;
                }

                // 4) 이제 첫 번째 요청을 끝내고 200을 받게 함
                slowRelease.countDown();
                int firstStatus = firstFuture.get(2, TimeUnit.SECONDS);

                // then
                assertThat(firstStatus).isEqualTo(200);
                assertThat(acceptedCount).isEqualTo(concurrentRequests - 1);
                assertThat(okCount).isEqualTo(0); // 락이 풀리기 전 요청들은 200이 나오면 안 됨
                assertThat(testController.getExecutionCount()).isEqualTo(1); // 비즈니스 로직 1번만 실행

            } finally {
                testController.clearSlowHooks();
                executor.shutdownNow();
                executor.awaitTermination(3, TimeUnit.SECONDS);
            }
        }

        @Test
        @DisplayName("성공 : 202 응답에는 Retry-After 헤더가 포함된다 (sleep 없는 안정형)")
        void shouldIncludeRetryAfterHeaderIn202Response_stable() throws Exception {
            // given
            String idempotencyKey = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("retry", 100);

            CountDownLatch slowEntered = new CountDownLatch(1);
            CountDownLatch slowRelease = new CountDownLatch(1);
            testController.initSlowHooks(slowEntered, slowRelease);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                // 1) 첫 요청 시작 (락 획득 + handler 진입)
                executor.submit(() -> {
                    mockMvc.perform(post("/test/idempotency/slow")
                                    .header("Idempotency-Key", idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();
                    return null;
                });

                // 2) 첫 요청이 slowOperation에 진입(=락 획득 완료)할 때까지 기다림
                assertThat(slowEntered.await(2, TimeUnit.SECONDS))
                        .as("첫 요청이 slowOperation에 진입하지 못함")
                        .isTrue();

                // when & then - 두 번째 요청은 반드시 202 + Retry-After
                mockMvc.perform(post("/test/idempotency/slow")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isAccepted())
                        .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

            } finally {
                // 첫 요청 종료
                slowRelease.countDown();
                testController.clearSlowHooks();

                executor.shutdownNow();
                executor.awaitTermination(3, TimeUnit.SECONDS);
            }
        }
    }

    @Nested
    @DisplayName("캐시 재사용 테스트")
    class CacheReuseTest {

        @Test
        @DisplayName("성공 : 캐시된 결과는 여러 번 재사용할 수 있다")
        void shouldReuseCachedResultMultipleTimes() throws Exception {
            // given
            String idempotencyKey = UUID.randomUUID().toString();
            TestRequest request = new TestRequest("reuse", 100);
            testController.resetExecutionCount();

            // 첫 번째 요청
            MvcResult firstResult = mockMvc.perform(post("/test/idempotency/create")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String firstId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                    .get("id").asText();

            // when - 5번 추가 요청
            for (int i = 0; i < 5; i++) {
                MvcResult result = mockMvc.perform(post("/test/idempotency/create")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andReturn();

                String id = objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText();
                assertThat(id).isEqualTo(firstId);
            }

            // then
            assertThat(testController.getExecutionCount()).isEqualTo(1); // 비즈니스 로직은 1번만 실행
        }
    }
}