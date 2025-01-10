package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.concert.model.ConcertSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "CONCERT_SCHEDULE")
public class ConcertScheduleJpaEntity extends BaseJpaEntity {
    @Id
    @Column(name = "concert_schedule_id")
    private String concertScheduleId;
    private String concertId;
    private LocalDateTime datetime;
    private LocalDateTime reservationStartAt;
    private LocalDateTime reservationEndAt;

    public static ConcertScheduleJpaEntity fromDomainModel(ConcertSchedule domainModel) {
        ConcertScheduleJpaEntity entity = new ConcertScheduleJpaEntity();
        entity.concertScheduleId = domainModel.getId();
        entity.concertId = domainModel.getConcertId();
        entity.datetime = domainModel.getDateTime();
        entity.reservationStartAt = domainModel.getReservationStartAt();
        entity.reservationEndAt = domainModel.getReservationEndAt();

        return entity;
    }

    public ConcertSchedule toDomainModel(){
        return ConcertSchedule.create(
                this.concertScheduleId,
                this.concertId,
                this.datetime,
                this.reservationStartAt,
                this.reservationEndAt);
    }
}
