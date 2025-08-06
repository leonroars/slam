package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.concert.model.ConcertSchedule;
import com.slam.concertreservation.domain.concert.model.ConcertScheduleAvailability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "CONCERTSCHEDULE"
        , indexes = @Index(name = "IDX_RESERVATION_AVAILABLE_PERIOD", columnList = "reservationStartAt, reservationEndAt"))
@Getter
public class ConcertScheduleJpaEntity extends BaseJpaEntity{

    @Id
    @Column(name = "concert_schedule_id")
    private String concertScheduleId;
    private String concertId;
    private String availability;
    private LocalDateTime datetime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;

    public static ConcertScheduleJpaEntity fromDomain(ConcertSchedule domainModel) {
        ConcertScheduleJpaEntity entity = new ConcertScheduleJpaEntity();
        entity.concertScheduleId = domainModel.getId();
        entity.concertId = domainModel.getConcertId();
        entity.availability = domainModel.getAvailability().name();
        entity.datetime = domainModel.getDateTime();
        entity.reservationStartAt = domainModel.getReservationStartAt();
        entity.reservationEndAt = domainModel.getReservationEndAt();

        return entity;
    }

    public ConcertSchedule toDomain(){
        return ConcertSchedule.create(
                this.concertScheduleId,
                this.concertId,
                this.datetime,
                this.reservationStartAt,
                this.reservationEndAt,
                ConcertScheduleAvailability.valueOf(this.availability));
    }

    public ConcertScheduleJpaEntity updateFromDomain(ConcertSchedule concertSchedule) {
        this.concertId = concertSchedule.getConcertId();
        this.availability = concertSchedule.getAvailability().name();
        this.datetime = concertSchedule.getDateTime();
        this.reservationStartAt = concertSchedule.getReservationStartAt();
        this.reservationEndAt = concertSchedule.getReservationEndAt();

        return this;
    }
}
