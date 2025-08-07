package com.slam.concertreservation.infrastructure.persistence.redis.impl;

import com.slam.concertreservation.domain.queue.model.Token;
import com.slam.concertreservation.domain.queue.repository.TokenRepository;
import com.slam.concertreservation.exceptions.UnavailableRequestException;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.queue.provider", havingValue = "redis", matchIfMissing = false)
public class TokenRepositoryRedisImpl implements TokenRepository {

    private static final String TOKEN_HASH_STORAGE_NAME = "tokenHashStorage"; // 토큰 저장소(Map) 이름
    private static final String TOKEN_RANK_SORTED_SET_NAME = "tokenRankSortedSet"; // 토큰 대기열 이름
    private static final String TOKEN_ACTIVATED_SET_NAME = "tokenActivatedSet"; // 활성화된 토큰 저장소(Set) 이름

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Token> tokenRedisTemplate;

    private final ZSetOperations<String, String> tokenScoredSortedSet;
    private final HashOperations<String, String, Token> tokenHashStorage;
    private final SetOperations<String, String> activatedTokenSet;

    public TokenRepositoryRedisImpl(StringRedisTemplate stringRedisTemplate,
                                    RedisTemplate<String, Token> tokenRedisTemplate) {

        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenRedisTemplate = tokenRedisTemplate;
        this.tokenScoredSortedSet = stringRedisTemplate.opsForZSet();
        this.tokenHashStorage = tokenRedisTemplate.opsForHash();
        this.activatedTokenSet = stringRedisTemplate.opsForSet();
    }

    /**
     * 토큰의 생성 시점을 점수(Rank)로 환산해주는 메서드.
     * @param token
     * @return
     */
    private double calculateScoreFromCreatedTime(Token token) {
        return token.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    /**
     * Redis 내 해당 공연 일정과 대응하는 대기열 이름을 생성하여 반환하는 메서드.
     * <br></br>
     * 이 문자열 반환값이 바로 Redis 내 대기열(Scored Sorted Set)의 식별자가 됩니다.
     * <br></br>
     * 해당 식별자에 공연 일정 ID를 함께 넣어 생성함으로써 공연 일정 별로 각각 구분된 대기열이 생성되어 관리되도록 합니다.
     * <br></br>
     * i.e. {@code tokenRankSortedSet:concertScheduleId}
     * @param concertScheduleId
     * @return
     */
    private String getTokenRankSortedSetName(String concertScheduleId) {
        return TOKEN_RANK_SORTED_SET_NAME + ":" + concertScheduleId;
    }

    /**
     * Redis 내 해당 공연 일정과 대응하는 토큰 저장소 이름을 생성하여 반환하는 메서드.
     * <br></br>
     * 이 문자열 반환값이 바로 Redis 내 토큰 저장소(Hash)의 식별자가 됩니다.
     * <br></br>
     * 해당 식별자에 공연 일정 ID를 함께 넣어 생성함으로써 공연 일정 별로 각각 구분된 토큰 저장소가 생성되어 관리되도록 합니다.
     * @param concertScheduleId
     * @return
     */
    private String getTokenHashStorageName(String concertScheduleId) {
        return TOKEN_HASH_STORAGE_NAME + ":" + concertScheduleId;
    }

    /**
     * Redis 내 해당 공연 일정과 대응하는 활성화된 토큰 저장소 이름을 생성하여 반환하는 메서드.
     * @param concertScheduleId
     * @return
     */
    private String getTokenActivatedSetName(String concertScheduleId) {
        return TOKEN_ACTIVATED_SET_NAME + ":" + concertScheduleId;
    }

    @Override
    public Token save(Token token) {

        // Token ID 생성.
        token.assignId(UUID.randomUUID().toString());

        // 토큰 저장소 이름과 대기열 이름 생성.
        String tokenHashStorageName = getTokenHashStorageName(token.getConcertScheduleId());
        String tokenRankSortedSetName = getTokenRankSortedSetName(token.getConcertScheduleId());

        // 토큰 저장소에 토큰 저장.
        tokenHashStorage.put(tokenHashStorageName, token.getId(), token);

        // 대기열에 토큰 추가.
        tokenScoredSortedSet.add(tokenRankSortedSetName, token.getId(), calculateScoreFromCreatedTime(token));

        // 토큰 저장소에 보관된 토큰 저장.
        return tokenHashStorage.get(tokenHashStorageName, token.getId());
    }

    /**
     * 만료 처리된 토큰들 / 활성화 처리된 토큰들을 받아 한 번에 저장.
     * <br></br>
     * 토큰 목록이 비어있을 경우 비어있는 목록을 반환합니다.
     * @param tokens
     * @return
     */
    @Override
    public List<Token> saveAll(List<Token> tokens) {
        if(tokens != null && !tokens.isEmpty()) {
            for(Token token : tokens){
                // pipelining 하지 않기 때문에 null 이 반환되지 않는다.
                // 기존의 ACTIVE 상태인 토큰을 삭제하고 만료 상태인 해당 토큰으로 교체하기.
                String setKey = getTokenActivatedSetName(token.getConcertScheduleId());
                String tokenId = token.getId();
                if(activatedTokenSet.isMember(setKey, tokenId)) {
                    activatedTokenSet.remove(setKey, tokenId);
                }
                // 만료 처리된 토큰 / 활성화된 토큰 저장.
                activatedTokenSet.add(setKey, tokenId);
            }
        }
        return List.of(); // 좋은 설계가 아닌 거 같다. 조용히 무시되도록 구현하는 쪽이 가장 좋을 것으로 생각.
    }

    /**
     * 공연 일정 ID 와 토큰 ID 로 토큰을 조회하는 메서드.
     * @param concertScheduleId
     * @param tokenId
     * @return
     */
    @Override
    public Optional<Token> findTokenWithIdAndConcertScheduleId(String concertScheduleId, String tokenId) {
        return Optional.ofNullable(tokenHashStorage.get(getTokenHashStorageName(concertScheduleId), tokenId));
    }

    /**
     * 특정 공연 일정의 대기열에서 다음으로 활성화할 토큰 K 개를 조회하는 메서드.
     * @param concertScheduleId
     * @param k
     * @return
     */
    @Override
    public List<Token> findNextKTokensToBeActivated(String concertScheduleId, int k) {
        Set<ZSetOperations.TypedTuple<String>> toBeActivated
                = Optional.ofNullable(tokenScoredSortedSet.popMin(getTokenRankSortedSetName(concertScheduleId),k))
                .orElseThrow(() -> new UnavailableRequestException("대기 중인 토큰이 존재하지 않습니다."));

        return toBeActivated
                .stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(id -> tokenHashStorage.get(getTokenHashStorageName(concertScheduleId), id))
                .toList();
    }

    /**
     * 해당 공연 일정의 현재 활성화된 토큰 수를 조회하는 메서드. 이는 곧 "서비스 진입 하여 이용 중인 사용 자 수"를 의미합니다.
     * @param concertScheduleId
     * @return
     */
    @Override
    public int countCurrentlyActiveTokens(String concertScheduleId) {
        Long activeTokenCount = activatedTokenSet.size(getTokenActivatedSetName(concertScheduleId));
        // Operation Pipelining 하지 않고 단일 명령 호출이므로 null 이 발생하지 않는다.
        return activeTokenCount.intValue();
    }

    /**
     * 공연 일정 ID, 사용자 ID, 상태 목록으로 특정 토큰 보유자의 순서를 조회하는 메서드.
     * @param concertScheduleId
     * @param tokenId
     * @return
     */
    @Override
    public int countRemaining(String concertScheduleId, String tokenId) {
        // 토큰 발급 이력 조회.
        Token targetToken
                = findTokenWithIdAndConcertScheduleId(concertScheduleId, tokenId)
                .orElseThrow(() -> new UnavailableRequestException("해당 토큰의 발급 이력이 존재하지 않습니다."));

        // 활성화된 토큰 여부인지 먼저 확인.
        if(activatedTokenSet.isMember(getTokenActivatedSetName(concertScheduleId), tokenId)){
            return 0; // 활성화된 토큰은 대기열에서의 순위를 조회할 수 없다.
        }
        // 활성화되지 않은 토큰이라면 대기열에서의 순위 조회.
        else {
            Long rank = tokenScoredSortedSet.rank(getTokenRankSortedSetName(concertScheduleId), tokenId);
            return rank.intValue();
        }
    }

    /**
     * 활성화된 토큰 중 만료될 토큰을 조회하는 메서드.
     * @return
     */
    @Override
    public List<Token> findActivatedTokensToBeExpired(String concertScheduleId) {
        List<Token> toBeExpiredActivatedTokens = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1) Cursor를 활용해 활성화된 토큰을 100개 단위로 Iteration.
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("*")
                .count(100)
                .build();

        // 특정 공연 일정의 대기열을 순회하는 Cursor 초기화.
        Cursor<String> cursor = activatedTokenSet.scan(getTokenActivatedSetName(concertScheduleId), scanOptions);

        try {
            while (cursor.hasNext()) {
                String tokenId = cursor.next();

                // 토큰 ID로 토큰 조회.
                Token token = tokenHashStorage.get(getTokenHashStorageName(concertScheduleId), tokenId);

                if (token.getExpiredAt() != null && token.getExpiredAt().isBefore(now)) {
                    toBeExpiredActivatedTokens.add(token);
                }
            }
        } finally {
                cursor.close();
        }

        // You can remove expired tokens in a second pass or inline above
        return toBeExpiredActivatedTokens;
    }

    /**
     * 대기 중인 토큰 중 만료될 토큰을 조회하는 메서드.
     * <br></br>
     * 예약 종료 후에 Batch 성 작업으로 토큰 삭제 시에 호출할 것으로 예상.
     * @return
     */
    @Override
    public List<Token> findWaitingTokensToBeExpired() {return List.of();}

    /**
     * Redis 내 해당 공연 일정과 대응하는 토큰 저장소에서 토큰들을 조회하는 메서드.
     * <br></br>
     * 사실 상 이 메서드는 사용되지 않을 것으로 예상됩니다.
     * @param concertScheduleId
     * @return
     */
    @Override
    public List<Token> findByConcertScheduleId(String concertScheduleId) {
        return List.of();
    }

}
