package com.hhp7.concertreservation.infrastructure.persistence.jpa;

import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.TokenJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenJpaRepository extends JpaRepository<TokenJpaEntity, Long> {

    Optional<TokenJpaEntity> findByConcertScheduleIdAndUserIdAndStatus(String concertScheduleId, String userId, String status);

    List<TokenJpaEntity> findByConcertScheduleId(String concertScheduleId);

    @Query("SELECT t FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId "
            + "AND t.status = :status "
            + "ORDER BY t.id"
            + " ASC LIMIT :k")
    List<TokenJpaEntity> findTopKByConcertScheduleIdAndStatus(@Param("concertScheduleId") String concertScheduleId
            , @Param("status") String status
            , @Param("k") int k);

    @Query("SELECT COUNT(t) FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.status = :status")
    int countTokensByConcertScheduleIdAndStatus(@Param("concertScheduleId") String concertScheduleId, @Param("status") String status);

    @Query("SELECT COUNT(t) FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.id = :tokenId AND t.status = 'ACTIVE' OR t.status = 'WAIT'")
    int countRemainingByConcertScheduleIdAndTokenIdAndStatus(@Param("concertScheduleId") String concertScheduleId, @Param("tokenId") String tokenId);

    @Query("SELECT t FROM TokenJpaEntity t WHERE t.expiredAt < CURRENT_TIMESTAMP AND t.status = 'ACTIVE' OR t.status = 'WAIT'")
    List<TokenJpaEntity> findExpiredTokens();
}
