package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.reservation.model.Reservation;
import com.hhp7.concertreservation.domain.reservation.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "RESERVATION")
public class ReservationJpaEntity extends BaseJpaEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;
    private String userId;
    private String concertScheduleId;
    private String seatId;
    private String status;
    private LocalDateTime expiredAt;

    public Reservation toDomain(){
        return Reservation.create(
                String.valueOf(this.getId()),
                this.getUserId(),
                this.getSeatId(),
                this.getConcertScheduleId(),
                ReservationStatus.valueOf(this.getStatus()),
                this.getExpiredAt(),
                this.getCreated_at()
        );
    }

    public static ReservationJpaEntity fromDomain(Reservation reservation){
        ReservationJpaEntity entity = new ReservationJpaEntity();
        entity.id = Long.valueOf(reservation.getId());
        entity.userId = reservation.getUserId();
        entity.seatId = reservation.getSeatId();
        entity.concertScheduleId = reservation.getConcertScheduleId();
        entity.status = reservation.getStatus().name();
        entity.expiredAt = reservation.getExpiredAt();
        return entity;
    }
}
