package com.ys.locksmith.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("분산락 유틸리티 테스트")
class DistributedLockUtilTest {
    
    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RLock rLock;
    
    private DistributedLockUtil lockUtil;
    
    @BeforeEach
    void setUp() {
        lockUtil = new DistributedLockUtil(redissonClient);
    }
    
    @Test
    @DisplayName("락을 성공적으로 획득하고 작업을 수행한다")
    void executeWithLockSuccess() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        String expectedResult = "success";
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        
        // when
        String result = lockUtil.executeWithLock(lockKey, () -> expectedResult);
        
        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(rLock).tryLock(10, 30, TimeUnit.SECONDS);
        verify(rLock).unlock();
    }
    
    @Test
    @DisplayName("락 획득에 실패하면 예외가 발생한다")
    void executeWithLockFailure() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);
        
        // when & then
        assertThatThrownBy(() -> lockUtil.executeWithLock(lockKey, () -> "test"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("락을 획득할 수 없습니다");
    }
    
    @Test
    @DisplayName("작업 수행 중 예외가 발생해도 락이 해제된다")
    void executeWithLockExceptionHandling() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        RuntimeException testException = new RuntimeException("test exception");
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> lockUtil.executeWithLock(lockKey, () -> {
            throw testException;
        })).isEqualTo(testException);
        
        verify(rLock).unlock();
    }
    
    @Test
    @DisplayName("인터럽트가 발생하면 적절히 처리된다")
    void executeWithLockInterrupted() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
            .willThrow(new InterruptedException("interrupted"));
        
        // when & then
        assertThatThrownBy(() -> lockUtil.executeWithLock(lockKey, () -> "test"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("락 획득 중 인터럽트 발생");
    }
    
    @Test
    @DisplayName("Runnable 작업을 수행할 수 있다")
    void executeWithLockRunnable() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        boolean[] executed = {false};
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        
        // when
        lockUtil.executeWithLock(lockKey, () -> executed[0] = true);
        
        // then
        assertThat(executed[0]).isTrue();
        verify(rLock).unlock();
    }
    
    @Test
    @DisplayName("커스텀 시간 설정으로 락을 사용할 수 있다")
    void executeWithLockCustomTime() throws InterruptedException {
        // given
        String lockKey = "test-lock";
        String expectedResult = "success";
        
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        
        // when
        String result = lockUtil.executeWithLock(lockKey, 5, 15, TimeUnit.SECONDS, () -> expectedResult);
        
        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(rLock).tryLock(5, 15, TimeUnit.SECONDS);
        verify(rLock).unlock();
    }
}