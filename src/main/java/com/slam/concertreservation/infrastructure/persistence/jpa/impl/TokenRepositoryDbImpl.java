package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.model.TokenStatus;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.TokenJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.TokenJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@ConditionalOnProperty(name = "app.queue.provider", havingValue = "DB", matchIfMissing = true)
@Repository
@RequiredArgsConstructor
public class TokenRepositoryDbImpl implements TokenRepository {
    private final TokenJpaRepository tokenJpaRepository;

    @Override
    public Token save(Token token) {
        return tokenJpaRepository.save(TokenJpaEntity.fromDomain(token))
                .toDomain();
    }

    @Override
    public List<Token> saveAll(List<Token> tokens) {
        return tokenJpaRepository.saveAll(TokenJpaEntity.createTokenJpaEntitiesFromDomain(tokens))
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Token> findTokenWithIdAndConcertScheduleId(Long concertScheduleId, String tokenId) {
        return tokenJpaRepository.findTokenJpaEntityByConcertScheduleIdAndId(concertScheduleId, Long.parseLong(tokenId))
                .map(TokenJpaEntity::toDomain);
    }

    @Override
    public List<Token> findByConcertScheduleId(Long concertScheduleId) {
        return tokenJpaRepository.findByConcertScheduleId(concertScheduleId)
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Token> findNextKTokensToBeActivated(Long concertScheduleId, int k) {
        return tokenJpaRepository.findTopKByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT.name(), k)
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countCurrentlyActiveTokens(Long concertScheduleId) {
        return tokenJpaRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE.name());
    }

    @Override
    public int countRemaining(Long concertScheduleId, String tokenId) {
        return tokenJpaRepository.countRemainingByConcertScheduleIdAndTokenId(concertScheduleId,
                Long.parseLong(tokenId));
    }

    @Override
    public List<Token> findActivatedTokensToBeExpired(Long concertScheduleId) {
        return tokenJpaRepository.findActivatedTokensToBeExpired(LocalDateTime.now())
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Token> findWaitingTokensToBeExpired() {
        return tokenJpaRepository.findWaitingTokensToBeExpired(LocalDateTime.now())
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countCurrentlyWaitingTokens(Long concertScheduleId) {
        return tokenJpaRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT.name());
    }

    @Override
    public void setQueueExpiration(Long concertScheduleId, long ttlSeconds) {
    }
}
