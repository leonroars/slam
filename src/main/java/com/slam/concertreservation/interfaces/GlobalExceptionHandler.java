package com.slam.concertreservation.interfaces;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.exceptions.ConcurrencyException;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * ErrorCode 에 정의된 LogLevel 에 따라 적절한 로그 레벨로 로그를 기록하기 위한 맵핑.
     */
    private static final Map<LogLevel, BiConsumer<String, Object[]>> LOG_HANDLER = Map.of(
            LogLevel.INFO, log::info,
            LogLevel.WARN, log::warn,
            LogLevel.ERROR, log::error
    );

    private void logByLevel(ErrorCode errorCode, String message, Object... args) {
        LOG_HANDLER.get(errorCode.getLogLevel()).accept(message, args);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException e) {
        // 로그 기록 : ErrorCode 에 정의된 LogLevel 에 따라 적절한 레벨로 기록
        logByLevel(e.getErrorCode(),
                "Business Error - Code: {}, Detail: {}",
                e.getErrorCode().getCode(),
                e.getDetail());

        // ProblemDetail 생성 : Client 에게 전달할 표준화된 에러 응답
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(e.getErrorCode().getHttpStatus()),
                e.getErrorCode().getMessage() // Client 에게 서비스 내부 구조가 노출되지 않도록 일반화된 메시지 전달
        );

        // ProblemDetail Field 입력.
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setType(URI.create("/errors/" + e.getErrorCode().getCode()));
        problemDetail.setProperty("errorCode", e.getErrorCode().getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return problemDetail;
    }

    @ExceptionHandler(UnavailableRequestException.class)
    public ProblemDetail handleNotAvailableRequest(UnavailableRequestException e) {
        logByLevel(e.getErrorCode(),
                "Unavailable Request - Code: {}, Detail: {}",
                e.getErrorCode().getCode(),
                e.getDetail());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(e.getErrorCode().getHttpStatus()),
                e.getErrorCode().getMessage() // Client 에게 서비스 내부 구조가 노출되지 않도록 일반화된 메시지 전달
        );

        problemDetail.setTitle("Unavailable Request");
        problemDetail.setType(URI.create("/errors/" + e.getErrorCode().getCode()));
        problemDetail.setProperty("errorCode", e.getErrorCode().getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return problemDetail;
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ProblemDetail handleConcurrencyException(ConcurrencyException e) {
        logByLevel(e.getErrorCode(),
                "Concurrency Error - Code: {}, Detail: {}",
                e.getErrorCode().getCode(),
                e.getDetail());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(e.getErrorCode().getHttpStatus()),
                e.getErrorCode().getMessage() // Client 에게 서비스 내부 구조가 노출되지 않도록 일반화된 메시지 전달
        );

        problemDetail.setTitle("Concurrency Error");
        problemDetail.setType(URI.create("/errors/" + e.getErrorCode().getCode()));
        problemDetail.setProperty("errorCode", e.getErrorCode().getCode());
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return problemDetail;
    }

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException e) {
        logByLevel(ErrorCode.INTERNAL_SERVER_ERROR,
                "Unexpected Error - Code: {}, Detail: {}",
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage() // Client 에게 서비스 내부 구조가 노출되지 않도록 일반화된 메시지 전달
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("/errors/G500"));
        problemDetail.setProperty("errorCode", "G500");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}