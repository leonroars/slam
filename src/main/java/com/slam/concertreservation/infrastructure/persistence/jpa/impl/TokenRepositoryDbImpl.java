package com.slam.concertreservation.infrastructure.persistence.jpa.impl;

import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.model.TokenStatus;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.TokenJpaRepository;
import com.slam.concertreservation.infrastructure.persistence.jpa.entities.TokenJpaEntity;
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
    public Optional<Token> findTokenWithIdAndConcertScheduleId(String concertScheduleId, String tokenId) {
        return tokenJpaRepository.findTokenJpaEntityByConcertScheduleIdAndId(concertScheduleId, Long.parseLong(tokenId))
                .map(TokenJpaEntity::toDomain);
    }

    @Override
    public List<Token> findByConcertScheduleId(String concertScheduleId) {
        return tokenJpaRepository.findByConcertScheduleId(concertScheduleId)
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Token> findNextKTokensToBeActivated(String concertScheduleId, int k) {
        return tokenJpaRepository.findTopKByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.WAIT.name(), k)
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countCurrentlyActiveTokens(String concertScheduleId) {
        return tokenJpaRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, TokenStatus.ACTIVE.name());
    }

    @Override
    public int countRemaining(String concertScheduleId, String tokenId) {
        return tokenJpaRepository.countRemainingByConcertScheduleIdAndTokenId(concertScheduleId, Long.parseLong(tokenId));
    }

    @Override
    public List<Token> findActivatedTokensToBeExpired(String concertScheduleId) {
        return tokenJpaRepository.findActivatedTokensToBeExpired()
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Token> findWaitingTokensToBeExpired() {
        return tokenJpaRepository.findWaitingTokensToBeExpired()
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }
}
