package com.hhp7.concertreservation.infrastructure.persistence.redis;

import com.hhp7.concertreservation.domain.queue.model.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;

@Configuration
public class RedisOpsConfig {

    @Bean
    public ZSetOperations<String, Token> zSetOperations(RedisTemplate<String, Token> tokenRedisTemplate) {
        return tokenRedisTemplate.opsForZSet();
    }

    @Bean
    public HashOperations<String, String, Token> hashOperations(RedisTemplate<String, Token> tokenRedisTemplate) {
        return tokenRedisTemplate.opsForHash();
    }

    @Bean
    public SetOperations<String, Token> setOperations(RedisTemplate<String, Token> tokenRedisTemplate) {
        return tokenRedisTemplate.opsForSet();
    }
}
