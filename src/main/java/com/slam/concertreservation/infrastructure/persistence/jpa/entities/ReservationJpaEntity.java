package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.reservation.model.Reservation;
import com.slam.concertreservation.domain.reservation.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "`RESERVATION`")
public class ReservationJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "reservation_id")
    private Long id;
    private Long userId;
    private String concertScheduleId;
    private String seatId;
    private Integer price;
    private String status;
    private LocalDateTime expiredAt;

    public Reservation toDomain() {
        return Reservation.create(
                this.getId(),
                this.getUserId(),
                this.getSeatId(),
                this.getConcertScheduleId(),
                ReservationStatus.valueOf(this.getStatus()),
                this.getPrice(),
                this.getExpiredAt(),
                this.getCreated_at(),
                this.getUpdated_at());
    }

    public static ReservationJpaEntity fromDomain(Reservation reservation) {
        ReservationJpaEntity entity = new ReservationJpaEntity();
        entity.id = reservation.getId();
        entity.userId = reservation.getUserId();
        entity.seatId = reservation.getSeatId();
        entity.concertScheduleId = reservation.getConcertScheduleId();
        entity.status = reservation.getStatus().name();
        entity.price = reservation.getPrice();
        entity.expiredAt = reservation.getExpiredAt();
        return entity;
    }

    public ReservationJpaEntity updateFromDomain(Reservation domain) {
        this.id = domain.getId();
        this.userId = domain.getUserId();
        this.seatId = domain.getSeatId();
        this.concertScheduleId = domain.getConcertScheduleId();
        this.status = domain.getStatus().name();
        this.price = domain.getPrice();
        this.expiredAt = domain.getExpiredAt();
        return this;
    }
}
