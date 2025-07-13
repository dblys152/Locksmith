package com.ys.locksmith.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockUtil {
    
    private final RedissonClient redissonClient;
    
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            
            if (!isLocked) {
                log.warn("분산락 획득 실패: {}", lockKey);
                throw new IllegalStateException("락을 획득할 수 없습니다: " + lockKey);
            }
            
            log.debug("분산락 획득 성공: {}", lockKey);
            return supplier.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산락 대기 중 인터럽트 발생: {}", lockKey, e);
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산락 해제: {}", lockKey);
            }
        }
    }
    
    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }
    
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 10, 30, TimeUnit.SECONDS, supplier);
    }
    
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, 10, 30, TimeUnit.SECONDS, runnable);
    }
}