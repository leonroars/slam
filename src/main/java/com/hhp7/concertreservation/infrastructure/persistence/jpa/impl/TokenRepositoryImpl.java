package com.hhp7.concertreservation.infrastructure.persistence.jpa.impl;

import com.hhp7.concertreservation.domain.queue.model.Token;
import com.hhp7.concertreservation.domain.queue.model.TokenStatus;
import com.hhp7.concertreservation.domain.queue.repository.TokenRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.TokenJpaRepository;
import com.hhp7.concertreservation.infrastructure.persistence.jpa.entities.TokenJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TokenRepositoryImpl implements TokenRepository {
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
    public Optional<Token> findById(String tokenId) {
        return tokenJpaRepository.findById(Long.valueOf(tokenId))
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
    public Optional<Token> findByConcertScheduleIdAndUserIdAndStatus(
            String concertScheduleId, String userId, TokenStatus status) {
        return tokenJpaRepository.findByConcertScheduleIdAndUserIdAndStatus(
                concertScheduleId, userId, status.name())
                .map(TokenJpaEntity::toDomain);
    }

    @Override
    public List<Token> findNextKTokensByConcertScheduleIdAndStatus(String concertScheduleId, TokenStatus status, int k) {
        return tokenJpaRepository.findTopKByConcertScheduleIdAndStatus(concertScheduleId, status.name(), k)
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countTokensByConcertScheduleIdAndStatus(String concertScheduleId, TokenStatus status) {
        return tokenJpaRepository.countTokensByConcertScheduleIdAndStatus(concertScheduleId, status.name());
    }

    @Override
    public int countRemainingByConcertScheduleIdAndTokenIdAndStatus(String concertScheduleId, String tokenId) {
        return tokenJpaRepository.countRemainingByConcertScheduleIdAndTokenIdAndStatus(concertScheduleId, tokenId);
    }

    @Override
    public List<Token> findExpiredTokens() {
        return tokenJpaRepository.findExpiredTokens()
                .stream()
                .map(TokenJpaEntity::toDomain)
                .toList();
    }
}
