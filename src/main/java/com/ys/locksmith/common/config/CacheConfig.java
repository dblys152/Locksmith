package com.ys.locksmith.common.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();
        
        // 결제 정보 캐시 설정 (TTL: 5분)
        config.put("payment", new org.redisson.spring.cache.CacheConfig(5 * 60 * 1000, 2 * 60 * 1000));
        
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}