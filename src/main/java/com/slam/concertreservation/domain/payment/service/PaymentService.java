package com.slam.concertreservation.domain.payment.service;

import com.slam.concertreservation.common.error.ErrorCode;
import com.slam.concertreservation.common.exceptions.UnavailableRequestException;
import com.slam.concertreservation.domain.payment.model.Payment;
import com.slam.concertreservation.domain.payment.model.PaymentStatus;
import com.slam.concertreservation.domain.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment initiate(Long userId, int amount, Long reservationId) {
        Payment initiatedPayment = Payment.create(userId, amount, reservationId);
        return paymentRepository.save(initiatedPayment);
    }

    @Transactional
    public Payment complete(Payment payment) {
        return paymentRepository.save(payment.withStatus(PaymentStatus.COMPLETED));
    }

    public Payment fail(Payment payment) {
        return paymentRepository.save(payment.withStatus(PaymentStatus.FAILED));
    }

    public Payment refund(Payment payment) {
        return paymentRepository.save(payment.withStatus(PaymentStatus.REFUNDED));
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new UnavailableRequestException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found for id: " + paymentId));
    }

    public Page<Payment> getPaymentHistoryOfUser(Long userId, Pageable pageable) {
        return paymentRepository.findAllByUserId(userId, pageable);
    }

    public List<Payment> getPaymentsByReservationId(Long reservationId) {
        return paymentRepository.findAllByReservationId(reservationId);
    }

    public List<Payment> getPaymentsByUserIdAndDateRange(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime)
    {
        return paymentRepository.findAllByUserIdAndCreatedAtBetween(userId, startDateTime, endDateTime);
    }

    public List<Payment> getPaymentsByDateRange(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime)
    {
        return paymentRepository.findAllByCreatedAtBetween(startDateTime, endDateTime);
    }
}
