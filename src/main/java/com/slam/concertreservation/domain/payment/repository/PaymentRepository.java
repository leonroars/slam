package com.slam.concertreservation.domain.payment.repository;

import com.slam.concertreservation.domain.payment.model.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long paymentId);

    List<Payment> findAllByReservationId(Long reservationId);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    List<Payment> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Payment> findAllByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

}
