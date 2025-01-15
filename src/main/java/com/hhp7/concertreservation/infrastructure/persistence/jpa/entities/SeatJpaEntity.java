package com.hhp7.concertreservation.infrastructure.persistence.jpa.entities;

import com.hhp7.concertreservation.domain.concert.model.Seat;
import com.hhp7.concertreservation.domain.concert.model.SeatStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Table(name = "SEAT")
@Getter
public class SeatJpaEntity extends BaseJpaEntity{
    @Id
    private String seatId;
    private String concertScheduleId;
    private int number;
    private int price;
    private String status;

    @Version
    private long version; // Row 단위 Optimistic Locking을 위한 Version 필드.

    public static SeatJpaEntity fromDomain(Seat seat) {
        SeatJpaEntity entity = new SeatJpaEntity();
        entity.seatId = seat.getId();
        entity.concertScheduleId = seat.getConcertScheduleId();
        entity.number = seat.getNumber();

        return entity;
    }

    public Seat toDomain() {
        return Seat.create(seatId, concertScheduleId, number, price, SeatStatus.valueOf(status));
    }

    /**
     * {@code List<Seat>} -> {@code List<SeatJpaEntity>}
     * @param seats
     * @return
     */
    public static List<SeatJpaEntity> createSeatEntitiesFromDomain(List<Seat> seats) {
        List<SeatJpaEntity> entities = new ArrayList<>();
        for (Seat seat : seats) {
            entities.add(SeatJpaEntity.fromDomain(seat));
        }
        return entities;
    }

}
