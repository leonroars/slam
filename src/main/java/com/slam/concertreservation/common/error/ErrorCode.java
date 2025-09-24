package com.slam.concertreservation.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;

/**
 * <b>SLAM! 콘서트 예약 시스템 에러 코드 정의</b>
 * <br></br>
 * <br></br>
 * - 각 에러 상황에 대한 고유한 코드와 메시지를 정의합니다.
 * <p>
 * - 에러 코드는 도메인, 타입, 순번으로 구성되어 있어 빠른 식별이 가능합니다.
 * <p>
 * - HTTP 상태 코드를 포함하여 클라이언트에게 적절한 응답을 제공합니다.
 * <p>
 * - ErrorCode에 정의된 error code 와 message 는 클라이언트에게 제공할 정보를 정의하는 것을 목표로 합니다.</p>
 *   따라서, 서비스 내부 구조를 노출하지 않기 위해 message는 최대한 일반적인 표현을 사용합니다.
 * <p>
 * - 내부 로직에서 발생하는 세부적인 예외 상황은 별도의 Exception 클래스를 통해 관리합니다.
 * <br></br>
 * <br></br>
 * 2. 코드 체계:
 * <p>
 * - [도메인][타입][순번] 형식
 * <p>
 * - 도메인: U(User), P(Point), C(Concert), S(Seat), R(Reservation), Q(Queue), G(General)
 * <p>
 * - 타입: 4xx(클라이언트 오류), 5xx(서버 오류)
 * <p>
 * - 순번: 001~999
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ==================== General Errors (G) ====================
    INTERNAL_SERVER_ERROR("G500", "요청을 처리할 수 없습니다. 잠시 후 다시 시도해주시기 바랍니다.", 500, LogLevel.ERROR),
    DOMAIN_RULE_VIOLATION("G501", "요청을 처리할 수 없습니다. 잠시 후 다시 시도해주시기 바랍니다.", 422, LogLevel.WARN),

    INVALID_INPUT_VALUE("G400", "잘못된 입력값입니다", 400, LogLevel.INFO),
    INVALID_REQUEST("Q400", "잘못된 요청입니다. 다시 시도해주시기 바랍니다.", 400, LogLevel.INFO),
    RESOURCE_NOT_FOUND("G404", "조회하고자 하는 자원이 존재하지 않습니다.", 404, LogLevel.INFO),
    TOO_MANY_REQUESTS("G429", "잠시 후 다시 시도해주시기 바랍니다.", 429, LogLevel.WARN),

    // ==================== User Domain (U) ====================
    USER_NOT_FOUND("U404", "해당 사용자가 존재하지 않습니다.", 404, LogLevel.INFO),

    // ==================== Point Domain (P) ====================
    // 잔액 정책 위반 (400번대)
    POINT_BELOW_ZERO("P400", "사용자는 0보다 작은 포인트 잔액을 가질 수 없습니다", 400, LogLevel.INFO),
    POINT_EXCEED_LIMIT("P401", "사용자의 보유 포인트 최대 한도는 1,000,000점 입니다", 400, LogLevel.INFO),
    POINT_CHARGE_AMOUNT_INVALID("P402", "충전하고자 하는 포인트는 0보다 커야 합니다", 400, LogLevel.INFO),
    POINT_USE_AMOUNT_INVALID("P403", "차감하고자 하는 포인트는 0보다 커야합니다", 400, LogLevel.INFO),

    // 잔액 부족 (422)
    INSUFFICIENT_BALANCE("P422", "포인트 잔액이 충분하지 않습니다.", 422, LogLevel.INFO),
    POINT_CHARGE_EXCEED_LIMIT("P424", "최대 한도를 초과하는 금액은 충전 불가합니다.", 422, LogLevel.INFO),

    // ==================== Concert & Schedule Domain (C) ====================
    // 공연 일정 조회 실패 (404)
    CONCERT_SCHEDULE_NOT_FOUND("C404", "해당 공연 일정이 존재하지 않습니다", 404, LogLevel.INFO),

    // 공연 일정 정책 위반 (400)
    INVALID_RESERVATION_PERIOD("C400", "예약 시작 일자는 예약 종료 일자보다 늦을 수 없습니다", 400, LogLevel.INFO),
    INVALID_CONCERT_DATE_BEFORE_RESERVATION("C401", "공연 일자가 예약 가능 시작 일자에 선행합니다", 400, LogLevel.INFO),
    INVALID_CONCERT_DATE_DURING_RESERVATION("C402", "예약 가능 기간 중엔 공연이 종료될 수 없습니다", 400, LogLevel.INFO),

    // ==================== Seat Domain (S) ====================
    // 좌석 조회 실패 (404)
    SEAT_NOT_FOUND("S404", "해당 좌석이 존재하지 않습니다", 404, LogLevel.INFO),
    NO_AVAILABLE_SEATS("S405", "예약 가능한 좌석이 존재하지 않습니다", 404, LogLevel.INFO),

    // 좌석 정책 위반 (400)
    INVALID_SEAT_NUMBER("S400", "존재하지 않는 좌석입니다", 400, LogLevel.INFO),

    // 좌석 상태 충돌 (409)
    SEAT_ALREADY_OCCUPIED("S409", "이미 선점되었거나 이용 불가한 좌석입니다", 409, LogLevel.INFO),

    // ==================== Reservation Domain (R) ====================
    // 예약 조회 실패 (404)
    RESERVATION_NOT_FOUND("R404", "해당 예약이 존재하지 않습니다", 404, LogLevel.INFO),

    // 예약 충돌 (409)
    RESERVATION_ALREADY_EXISTS("R409", "해당 좌석은 이미 선점되었습니다.", 409, LogLevel.INFO),

    // 예약 만료
    RESERVATION_EXPIRED("R410", "예약 유효 시간이 만료되었습니다. 다시 예약을 시도해주시기 바랍니다.", 410, LogLevel.INFO),

    // ==================== Queue & Token Domain (Q) ====================
    TOKEN_NOT_FOUND("Q404", "문제가 발생했습니다. 다시 접속해주시기 바랍니다.", 404, LogLevel.INFO),
    TOKEN_EXPIRED("Q410", "잘못된 접근입니다. 다시 예약을 시도해주시기 바랍니다.", 410, LogLevel.WARN),

    // ==================== Concurrency (CC) ====================
    OPTIMISTIC_LOCK_CONFLICT("CC409", "문제가 발생했습니다. 잠시 후 다시 시도해주시기 바랍니다.", 409, LogLevel.WARN),

    // ==================== Persistence Error (DB) ====================
    CONCERT_SCHEDULE_SAVE_FAILED("DB500", "문제가 발생했습니다. 잠시 후 다시 시도해주시기 바랍니다.", 500, LogLevel.ERROR);

    private final String code;
    private final String message;
    private final int httpStatus;
    private final LogLevel logLevel;
}