package com.slam.concertreservation.domain.point.api;

public record PointOperationResult(
        boolean success,
        Long userId,
        int amount,
        String errorCode
) {
    public static PointOperationResult success(Long userId, int amount) {
        return new PointOperationResult(true, userId, amount, null);
    }

    public static PointOperationResult fail(Long userId, int amount, String errorCode) {
        return new PointOperationResult(false, userId, amount, errorCode);
    }
}
