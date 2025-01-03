package com.hhp7.concert.mockapi;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock API 컨트롤러
 */
@RestController
@RequestMapping("/api")
public class MockApiController {

    /**
     * 대기 상태 확인 API
     * <br></br>
     * Endpoint : GET /api/queue/status
     * <p>사용자가 현재 대기열에서 몇 번째인지, 혹은 자리가 비어 있는지 확인합니다.</p>
     * - 폴링 흉내를 내기 위해 다음과 같이 작동합니다!
     * - 이 예시는 3회 호출 전까지는 남은 순번을 하나씩 줄여서 반환하고
     * - 3번째 호출 이후에는 지정 날짜 좌석 선택 API(/api/seat/available)로 302 리다이렉트 합니다..
     *
     * @return 대기열 상태를 설명하는 단순 문자열 예시 또는 리다이렉트 응답
     */
    @GetMapping("/queue/status")
    public ResponseEntity<?> getQueueStatus(@RequestParam(value="date", required=false) String date,
                                            @RequestParam(value="userId", required=false) String userId) {
        int pollCount = 3;

        // 3회 이하일 때: 순번을 줄여서 표시
        if (pollCount > 0) {
            int queueNumber = pollCount--; // 예: 처음 호출 시 3번, 다음 호출 시 2번...
            String msg = "현재 대기열 순번은 " + queueNumber + "번이며, 대기 중입니다.";
            return new ResponseEntity<>(msg, HttpStatus.OK);
        }
        // 3회째(또는 그 이상)인 경우: "본인 차례"로 간주하고 리다이렉트
        else {
            // 한글/특수문자 대응 위해 URLEncoder 사용 예시
            String encodedDate = date != null
                    ? URLEncoder.encode(date, StandardCharsets.UTF_8)
                    : "";
            String encodedUserId = userId != null
                    ? URLEncoder.encode(userId, StandardCharsets.UTF_8)
                    : "";

            // 예: /api/seat/available?date=2025-01-01&userId=user123
            String redirectUrl = "/api/seat/available?date=" + encodedDate + "&userId=" + encodedUserId;
            // 지정 날짜 좌석 선택 API로 이동
            return ResponseEntity
                    .status(HttpStatus.FOUND) // 302 Found
                    .location(URI.create("/api/seat/available")) // 리다이렉트 URL
                    .build();
        }
    }

    /**
     * 대기열 토큰 생성 API
     * <br></br>
     * Endpoint : GET /api/queue/token
     * <p>대기열 최초 진입 시 토큰을 생성하여 발급합니다.</p>
     * @return 대기열 토큰 ID를 나타내는 문자열
     */
    @GetMapping("/queue/token")
    public ResponseEntity<String> createQueueToken() {
        String mockToken = "mock-queue-token-123456";
        return new ResponseEntity<>(mockToken, HttpStatus.OK);
    }

    /**
     * 예약 가능 날짜 조회 API
     * <br>
     * Endpoint : GET /api/dates/available
     * <p>특정 콘서트에 대해 예약할 수 있는 날짜 목록을 반환합니다.</p>
     * @return 예매 가능한 날짜 목록
     */
    @GetMapping("/dates/available")
    public ResponseEntity<String> getAvailableDates() {
        String mockDates = "[\"2025-01-10\", \"2025-01-11\", \"2025-01-12\"]";
        return new ResponseEntity<>(mockDates, HttpStatus.OK);
    }

    /**
     * 지정 날짜 예약 가능 좌석 조회 API
     * <br>
     * Endpoint : GET /api/seat/available
     * <p>특정 날짜의 예약 가능 좌석 목록을 반환합니다.</p>
     * @param date 요청 파라미터(date)
     * @return 해당 날짜의 예약 가능 좌석 목록(하드코딩)
     */
    @GetMapping("/seat/available")
    public ResponseEntity<String> getAvailableSeats(@RequestParam(value = "date", required = false) String date) {
        String mockSeats = "[\"A1\", \"A2\", \"A3\"]";
        // date 파라미터를 받아 처리할 수도 있지만 여기선 하드 코딩 예시로 처리합니다!
        return new ResponseEntity<>("날짜: " + date + " / 예약 가능 좌석: " + mockSeats, HttpStatus.OK);
    }

    /**
     * 좌석 예약 요청 API
     * <br>
     * Endpoint : POST /api/seat/reserve
     * <p>특정 좌석을 실제로 예약 요청합니다.</p>
     * @return 예약 성공/실패 여부
     */
    @PostMapping("/seat/reserve")
    public ResponseEntity<String> reserveSeat() {
        String mockResult = "좌석 예약 요청이 성공적으로 접수되었습니다. 5분간 HOLD 상태로 유지합니다. 시간 내 결제 미완료시 좌석은 다시 예약 가능 상태가 됩니다.";
        return new ResponseEntity<>(mockResult, HttpStatus.OK);
    }

    /**
     * 결제(포인트 차감) API
     * <br>
     * Endpoint : POST /api/payment
     * <p>포인트 잔액에서 티켓 가격만큼 차감하여 결제합니다.</p>
     * @return 결제 성공/실패 결과
     */
    @PostMapping("/payment")
    public ResponseEntity<String> doPayment() {
        // 간단 예시
        String mockPayment = "결제 완료. 좌석 예약이 확정되었습니다.";
        return new ResponseEntity<>(mockPayment, HttpStatus.OK);
    }

    /**
     * 포인트 충전 API
     * Endpoint : POST /api/point/charge
     * <p>사용자의 포인트를 원하는 금액만큼 충전합니다.</p>
     * @return 충전 성공/실패
     */
    @PostMapping("/point/charge")
    public ResponseEntity<String> chargePoint() {
        String mockResult = "포인트 충전이 완료되었습니다. 현재 잔액: 100000";
        return new ResponseEntity<>(mockResult, HttpStatus.OK);
    }

    /**
     * 포인트 잔액 조회 API
     * <br>
     * Endpoint : GET /api/point/balance
     * <p>사용자의 현재 포인트 잔액 정보를 조회합니다.</p>
     * @return 포인트 잔액 정보
     */
    @GetMapping("/point/balance")
    public ResponseEntity<String> getPointBalance() {
        String mockBalance = "현재 포인트 잔액: 70000";
        return new ResponseEntity<>(mockBalance, HttpStatus.OK);
    }
}