package com.hhp7.concertreservation.mockapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock API 컨트롤러
 * <p>요구사항 분석을 바탕으로,
 * 요청/응답 형식을 간략히 정의한 하드코딩 예시입니다.</p>
 */
@RestController
@RequestMapping("/api")
public class MockApiController {

    // 폴링 시뮬레이션을 위한 호출 횟수 카운터
    private final AtomicInteger pollCount = new AtomicInteger(0);

    /**
     * (I) 대기열 토큰 생성 API
     * <br>
     * Endpoint : GET /api/queue/token
     * <p>대기열 최초 진입 시 토큰을 생성하여 발급합니다.</p>
     *
     * @return 대기열 토큰 ID(String)
     *
     * <pre>
     * [Request]
     *   GET /api/queue/token
     *
     * [Response]
     *   HTTP 200
     *   Body: "mock-queue-token-123456" (예시)
     * </pre>
     */
    @GetMapping("/queue/token")
    public ResponseEntity<String> createQueueToken() {
        // 하드코딩된 예시
        String mockToken = "mock-queue-token-123456";
        return ResponseEntity.ok(mockToken);
    }

    /**
     * (II) 대기 상태 확인 API
     * <br>
     * Endpoint : GET /api/queue/status
     * <p>
     *  - "Cookie" 또는 "Query Param" 등을 통해 대기열 토큰을 전달받을 수 있다고 가정
     *  - 3회 미만 호출 시: 대기열 순번을 1씩 줄여가며 응답 (하드코딩)
     *  - 3회째(또는 그 이상) 호출 시: 본인 차례로 간주,
     *    "지정 날짜 예약 가능 좌석 조회 API"로 302 Redirect
     * </p>
     *
     * @param date   지정 날짜(yyyy-MM-dd 형태 가정)
     * @param userId 사용자 식별자
     * @param token  쿠키(또는 쿼리)로 전송되는 대기열 토큰
     * @return 대기열 상태(문자열) 혹은 리다이렉트 응답
     *
     * <pre>
     * [Request]
     *   GET /api/queue/status?date=2025-01-10&userId=user123
     *   Cookie: queueToken=mock-queue-token-123456 (예시)
     *
     * [Response]
     *  1) 아직 차례가 아닐 때 (최대 2번까지)
     *     200 OK
     *     Body: "현재 대기열 순번은 X번이며, 대기 중입니다."
     *  2) 3번째 호출 시
     *     302 Found
     *     Location: /api/seat/available?date=...&userId=...
     * </pre>
     */
    @GetMapping("/queue/status")
    public ResponseEntity<?> getQueueStatus(
            @RequestParam(value="date", required=false) String date,
            @RequestParam(value="userId", required=false) String userId,
            @CookieValue(name="queueToken", required=false) String token
    ) {
        // (실제 로직이라면 token 검증, userId 매핑, date 유효성 체크 등이 필요)
        int count = pollCount.incrementAndGet();

        // 3회 미만이면 -> 남은 순번 안내
        if (count < 3) {
            int queueNumber = 5 - count; // 예: 1회차 -> 4, 2회차 -> 3
            String msg = "현재 대기열 순번은 " + queueNumber + "번이며, 대기 중입니다. (date=" + date + ", userId=" + userId + ")";
            return ResponseEntity.ok(msg);
        }
        // 3회째(또는 그 이상) -> 본인 차례로 간주, 리다이렉트
        else {
            // 쿼리 파라미터로 date, userId 등을 함께 넘기기
            String encodedDate = (date == null) ? "" : URLEncoder.encode(date, StandardCharsets.UTF_8);
            String encodedUser = (userId == null) ? "" : URLEncoder.encode(userId, StandardCharsets.UTF_8);

            String redirectUrl = UriComponentsBuilder
                    .fromPath("/api/seat/available")
                    .queryParam("date", encodedDate)
                    .queryParam("userId", encodedUser)
                    .toUriString();

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }
    }

    /**
     * (III) 예약 가능 날짜 조회 API
     * <br>
     * Endpoint : GET /api/dates/available
     * <p>특정 콘서트에 대해 예약할 수 있는 날짜 목록을 반환합니다.</p>
     *
     * <pre>
     * [Request]
     *   GET /api/dates/available?concertId=xxx
     *   (토큰 유효성 체크 필요 시, 쿠키나 헤더로 전달)
     *
     * [Response]
     *   200 OK
     *   Body: ["2025-01-10", "2025-01-11", ...] (JSON 배열)
     * </pre>
     */
    @GetMapping("/dates/available")
    public ResponseEntity<String> getAvailableDates(
            @RequestParam(value="concertId", required=false) String concertId
    ) {
        // 하드코딩된 날짜 목록 (JSON 배열 문자열)
        String mockDates = "[\"2025-01-10\", \"2025-01-11\", \"2025-01-12\"]";
        return ResponseEntity.ok(mockDates);
    }

    /**
     * (IV) 지정 날짜 예약 가능 좌석 조회 API
     * <br>
     * Endpoint : GET /api/seat/available
     * <p>특정 날짜에 대해 예약 가능한 좌석 목록을 반환합니다.</p>
     *
     * <pre>
     * [Request]
     *   GET /api/seat/available?date=2025-01-10&userId=user123
     *   Cookie: queueToken=...
     *
     * [Response]
     *   200 OK
     *   Body 예시:
     *   {
     *     "date": "2025-01-10",
     *     "availableSeats": ["A1", "A2", "A3"]
     *   }
     *   (token 검증 실패 시 -> 401 or 403 에러)
     * </pre>
     */
    @GetMapping("/seat/available")
    public ResponseEntity<AvailableSeatsResponse> getAvailableSeats(
            @RequestParam(value="date", required=false) String date,
            @RequestParam(value="userId", required=false) String userId,
            @CookieValue(name="queueToken", required=false) String token
    ) {
        // (실제 로직이라면 token 유효성, date·userId 검증 로직이 필요)
        // 단순 하드코딩 응답
        AvailableSeatsResponse response = new AvailableSeatsResponse(
                (date == null) ? "2025-01-10" : date,
                new String[] {"A1", "A2", "A3"}
        );
        return ResponseEntity.ok(response);
    }

    /**
     * (V) 좌석 예약 요청 API
     * <br>
     * Endpoint : POST /api/seat/reserve
     * <p>특정 좌석을 가예약(HOLD) 상태로 전환하는 요청입니다.
     *    5분 내 결제가 이루어지지 않으면 만료 처리.</p>
     *
     * <pre>
     * [Request]
     *   POST /api/seat/reserve
     *   Cookie: queueToken=...
     *   Body (JSON):
     *     {
     *       "userId": "user123",
     *       "concertId": "CONCERT-001",
     *       "seatId": "A1"
     *     }
     *
     * [Response]
     *   200 OK (가예약 성공)
     *   Body 예시:
     *     {
     *       "status": "SUCCESS",
     *       "message": "좌석 예약 요청이 성공적으로 접수되었습니다. 5분간 HOLD 상태..."
     *     }
     *
     *   (실패 시 예: 잔여 좌석 없음, 이미 HOLD 상태, 토큰 검증 실패 -> 400 or 403 등)
     * </pre>
     */
    @PostMapping("/seat/reserve")
    public ResponseEntity<SeatReserveResponse> reserveSeat(
            @CookieValue(name="queueToken", required=false) String token,
            @RequestBody SeatReserveRequest request
    ) {
        // (실제 로직이라면 token, seat 중복예약 여부, user/concert 유효성 검증 등)
        SeatReserveResponse response = new SeatReserveResponse(
                "SUCCESS",
                String.format("좌석 예약 요청이 성공적으로 접수되었습니다. (user=%s, seat=%s) 5분간 HOLD 유지...",
                        request.getUserId(), request.getSeatId())
        );
        return ResponseEntity.ok(response);
    }

    /**
     * (VI) 결제(포인트 차감) API
     * <br>
     * Endpoint : POST /api/payment
     * <p>포인트 잔액에서 티켓 가격만큼 차감하여 결제합니다.</p>
     *
     * <pre>
     * [Request]
     *   POST /api/payment
     *   Cookie: queueToken=...
     *   Body (JSON):
     *     {
     *       "userId": "user123",
     *       "concertId": "CONCERT-001",
     *       "date": "2025-01-10",
     *       "seatId": "A1"
     *     }
     *
     * [Response]
     *   200 OK (결제 성공 시)
     *   {
     *     "status": "SUCCESS",
     *     "message": "결제 완료. 좌석 예약이 확정되었습니다."
     *   }
     *
     *   (실패 시: 잔액부족, 토큰 만료 등 -> 400 or 403 등)
     * </pre>
     */
    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> doPayment(
            @CookieValue(name="queueToken", required=false) String token,
            @RequestBody PaymentRequest request
    ) {
        // (실제 로직이라면 잔액 검증, seat HOLD 상태 확인 등)
        PaymentResponse response = new PaymentResponse(
                "SUCCESS",
                "결제 완료. 좌석 예약이 확정되었습니다."
        );
        return ResponseEntity.ok(response);
    }

    /**
     * (VII) 포인트 충전 API
     * <br>
     * Endpoint : POST /api/point/charge
     * <p>사용자의 포인트를 원하는 금액만큼 충전합니다.</p>
     *
     * <pre>
     * [Request]
     *   POST /api/point/charge
     *   Body (JSON):
     *     {
     *       "userId": "user123",
     *       "chargeAmount": 50000
     *     }
     *
     * [Response]
     *   200 OK (충전 성공 시)
     *   {
     *     "status": "SUCCESS",
     *     "message": "포인트 충전이 완료되었습니다. 현재 잔액: 100000"
     *   }
     *
     *   (실패 시: 사용자 없음, 한도 초과 등 -> 400 등)
     * </pre>
     */
    @PostMapping("/point/charge")
    public ResponseEntity<CommonResponse> chargePoint(
            @RequestBody ChargePointRequest request
    ) {
        // (실제 로직: user 검증, 잔액 한도 확인, 충전 처리 등)
        String msg = String.format("포인트 충전이 완료되었습니다. 현재 잔액: %d", 100000);
        return ResponseEntity.ok(new CommonResponse("SUCCESS", msg));
    }

    /**
     * (VIII) 포인트 잔액 조회 API
     * <br>
     * Endpoint : GET /api/point/balance
     * <p>사용자의 현재 포인트 잔액 정보를 조회합니다.</p>
     *
     * <pre>
     * [Request]
     *   GET /api/point/balance?userId=user123
     *
     * [Response]
     *   200 OK
     *   {
     *     "userId": "user123",
     *     "balance": 70000
     *   }
     *
     *   (실패 시: 사용자 없음 -> 404 등)
     * </pre>
     */
    @GetMapping("/point/balance")
    public ResponseEntity<PointBalanceResponse> getPointBalance(
            @RequestParam("userId") String userId
    ) {
        // (실제 로직이라면 userId가 유효한지 조회해야 함)
        PointBalanceResponse response = new PointBalanceResponse(userId, 70000);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 아래는 요청/응답에 사용될 간단한 내부 DTO 클래스들 (Mock 용)
    // =========================================================================

    /**
     * 지정 날짜 예약 가능 좌석 조회 응답 예시
     */
    public static class AvailableSeatsResponse {
        private String date;
        private String[] availableSeats;

        public AvailableSeatsResponse(String date, String[] availableSeats) {
            this.date = date;
            this.availableSeats = availableSeats;
        }
        public String getDate() { return date; }
        public String[] getAvailableSeats() { return availableSeats; }
    }

    /**
     * 좌석 예약 요청(가예약) Body
     */
    public static class SeatReserveRequest {
        private String userId;
        private String concertId;
        private String seatId;

        public String getUserId() { return userId; }
        public String getConcertId() { return concertId; }
        public String getSeatId() { return seatId; }
    }

    /**
     * 좌석 예약 요청(가예약) 응답
     */
    public static class SeatReserveResponse {
        private String status;   // ex) "SUCCESS", "FAIL"
        private String message;

        public SeatReserveResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    /**
     * 결제 요청 Body
     */
    public static class PaymentRequest {
        private String userId;
        private String concertId;
        private String date;   // "2025-01-10" 등
        private String seatId;

        public String getUserId() { return userId; }
        public String getConcertId() { return concertId; }
        public String getDate() { return date; }
        public String getSeatId() { return seatId; }
    }

    /**
     * 결제 응답
     */
    public static class PaymentResponse {
        private String status;   // ex) "SUCCESS", "FAIL"
        private String message;

        public PaymentResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    /**
     * 포인트 충전 요청
     */
    public static class ChargePointRequest {
        private String userId;
        private int chargeAmount;

        public String getUserId() { return userId; }
        public int getChargeAmount() { return chargeAmount; }
    }

    /**
     * 공통 응답 구조(포인트 충전 등)
     */
    public static class CommonResponse {
        private String status;   // "SUCCESS"/"FAIL"
        private String message;

        public CommonResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    /**
     * 포인트 잔액 조회 응답
     */
    public static class PointBalanceResponse {
        private String userId;
        private int balance;

        public PointBalanceResponse(String userId, int balance) {
            this.userId = userId;
            this.balance = balance;
        }
        public String getUserId() { return userId; }
        public int getBalance() { return balance; }
    }
}