package com.slam.concertreservation.common.exceptions;

import com.slam.concertreservation.common.error.ErrorCode;

public class ConcurrencyException extends RuntimeException implements IServiceException {

    private final ErrorCode errorCode;
    private final String detail;

    public ConcurrencyException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    @Override
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    @Override
    public String getDetail() {
        return this.detail;
    }
}
