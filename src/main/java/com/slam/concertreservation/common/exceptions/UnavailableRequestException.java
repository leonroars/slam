package com.slam.concertreservation.common.exceptions;

import com.slam.concertreservation.common.error.ErrorCode;

/**
 * 성립할 수 없는 요청에 대해 발생하는 예외입니다.500 계열의 에러코드와 대응하는 문제를 위해 정의하였습니다.
 * <br></br>
 * <i>성립할 수 없는 요청</i>은 다음으로 정의됩니다.
 * <br></br>
 * - 예약 가능한 시점이 아닐 때 예약을 시도하는 경우.
 * <br>
 * - 이미 예약된 좌석에 대한 예약을 시도하는 경우
 * <br>
 * -
 */
public class UnavailableRequestException extends RuntimeException implements IServiceException {

    private final ErrorCode errorCode;
    private final String detail;

    public UnavailableRequestException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    @Override
    public ErrorCode getErrorCode() {return this.errorCode;}

    @Override
    public String getDetail() {return this.detail;}
}
