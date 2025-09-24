package com.slam.concertreservation.common.exceptions;

/**
 * 성립할 수 없는 요청에 대해 발생하는 예외입니다.
 * <br></br>
 * <i>성립할 수 없는 요청</i>은 다음으로 정의됩니다.
 * <br></br>
 * - 예약 가능한 시점이 아닐 때 예약을 시도하는 경우.
 * <br>
 * - 이미 예약된 좌석에 대한 예약을 시도하는 경우
 * <br>
 * -
 */
public class UnavailableRequestException extends RuntimeException {
    public UnavailableRequestException(String message) {
        super(message);
    }
}
