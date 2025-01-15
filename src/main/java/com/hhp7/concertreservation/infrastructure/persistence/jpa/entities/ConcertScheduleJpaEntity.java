package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "CONCERTSCHEDULE")
@Getter
public class ConcertScheduleJpaEntity extends BaseJpaEntity{

    @Id
    @Column(name = "concert_schedule_id")
    private String concertScheduleId;
    private String concertId;
    private int availableSeatsCount;
    private LocalDateTime datetime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;

    @Version
    private long version; // Row 단위 Optimistic Locking을 위한 Version 필드.

    public static ConcertScheduleJpaEntity fromDomain(ConcertSchedule domainModel) {
        ConcertScheduleJpaEntity entity = new ConcertScheduleJpaEntity();
        entity.concertScheduleId = domainModel.getId();
        entity.concertId = domainModel.getConcertId();
        entity.availableSeatsCount = domainModel.getAvailableSeatCount();
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
                this.reservationEndAt);
    }
}
