package com.slam.concertreservation.interfaces;

import com.slam.concertreservation.common.exceptions.BusinessRuleViolationException;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(UnavailableRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNotAvailableRequest(UnavailableRequestException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOptimisticLockException(OptimisticLockingFailureException ex) {
        return "동시성 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException ex) {
        return "서버 에러가 발생했습니다: " + ex.getMessage();
    }
}