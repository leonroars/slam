package com.slam.concertreservation.common.exceptions;

import com.slam.concertreservation.common.error.ErrorCode;

/**
 * Application 전반에 걸쳐 발생하는 서비스 예외의 공통 인터페이스입니다.
 * <br></br>
 * - Domain-specific 한 Issue 와 그 외의 RuntimeException 을 발생시키는 Issue 를 구분하고, ExceptionHandler에서 다르게 대응하기 위함.
 */

public interface IServiceException {
    ErrorCode getErrorCode();
    String getDetail();
}
