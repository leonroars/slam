package com.slam.concertreservation.infrastructure.persistence.jpa;

import com.slam.concertreservation.infrastructure.persistence.jpa.entities.TokenJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenJpaRepository extends JpaRepository<TokenJpaEntity, Long> {

        @Query("SELECT t FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.id = :tokenId")
        Optional<TokenJpaEntity> findTokenJpaEntityByConcertScheduleIdAndId(
                        @Param("concertScheduleId") Long concertScheduleId,
                        @Param("tokenId") Long tokenId);

        List<TokenJpaEntity> findByConcertScheduleId(Long concertScheduleId);

        @Query("SELECT t FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId "
                        + "AND t.status = :status "
                        + "ORDER BY t.id"
                        + " ASC LIMIT :k")
        List<TokenJpaEntity> findTopKByConcertScheduleIdAndStatus(@Param("concertScheduleId") Long concertScheduleId,
                        @Param("status") String status, @Param("k") int k);

        @Query("SELECT COUNT(t) FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.status = :status")
        int countTokensByConcertScheduleIdAndStatus(@Param("concertScheduleId") Long concertScheduleId,
                        @Param("status") String status);

        @Query("SELECT COUNT(t) FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.id < :tokenId AND t.status = 'WAIT' ORDER BY t.id ASC")
        int countRemainingByConcertScheduleIdAndTokenId(@Param("concertScheduleId") Long concertScheduleId,
                        @Param("tokenId") Long tokenId);

        @Query("SELECT t FROM TokenJpaEntity t WHERE t.expiredAt < :now AND t.status = 'ACTIVE'")
        List<TokenJpaEntity> findActivatedTokensToBeExpired(@Param("now") LocalDateTime now);

        @Query("SELECT t FROM TokenJpaEntity t WHERE t.expiredAt < :now AND t.status = 'WAIT'")
        List<TokenJpaEntity> findWaitingTokensToBeExpired(@Param("now") LocalDateTime now);

        @Query("SELECT COUNT(t) FROM TokenJpaEntity t WHERE t.concertScheduleId = :concertScheduleId AND t.status = 'WAIT'")
        int countCurrentlyWaitingTokens(@Param("concertScheduleId") Long concertScheduleId);
}
