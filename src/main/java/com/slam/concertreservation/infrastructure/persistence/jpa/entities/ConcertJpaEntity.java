package com.slam.concertreservation.infrastructure.persistence.jpa.entities;

import com.slam.concertreservation.domain.concert.model.Concert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "'CONCERT'")
@Getter
public class ConcertJpaEntity extends BaseJpaEntity{

    @Id
    private String concertId;
    private String name;
    private String artist;

    public static ConcertJpaEntity fromDomain(Concert concert) {
        ConcertJpaEntity entity = new ConcertJpaEntity();
        entity.concertId = concert.getId();
        entity.name = concert.getName();
        entity.artist = concert.getArtist();

        return entity;
    }

    public Concert toDomain(){
        return Concert.create(
                this.concertId,
                this.name,
                this.artist);
    }
}
