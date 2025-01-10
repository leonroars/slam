package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.model.SeatStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SEAT")
public class SeatJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "seat_id")
    private String seatId;

    private String concertScheduleId;
    private int number;
    private int price;
    private String status;

    public static SeatJpaEntity fromDomainModel(Seat seat) {
        SeatJpaEntity entity = new SeatJpaEntity();
        entity.seatId = seat.getId();
        entity.concertScheduleId = seat.getConcertScheduleId();
        entity.number = seat.getNumber();

        return entity;
    }

    public Seat toDomainModel() {
        return Seat.assign(seatId, concertScheduleId, number, price, SeatStatus.valueOf(status));
    }



}
