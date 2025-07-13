package com.ys.locksmith.common.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("분산락 AOP 테스트")
class DistributedLockAspectTest {
    
    @Mock
    private DistributedLockUtil lockUtil;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @Mock
    private Method method;
    
    @Mock
    private DistributedLock distributedLock;
    
    private DistributedLockAspect aspect;
    
    @BeforeEach
    void setUp() {
        aspect = new DistributedLockAspect(lockUtil);
    }
    
    @Test
    @DisplayName("정적 락 키로 AOP가 동작한다")
    void executeWithStaticLockKey() throws Throwable {
        // given
        String lockKey = "static-lock-key";
        String expectedResult = "success";
        
        given(distributedLock.key()).willReturn(lockKey);
        given(distributedLock.waitTime()).willReturn(10L);
        given(distributedLock.leaseTime()).willReturn(30L);
        given(distributedLock.timeUnit()).willReturn(TimeUnit.SECONDS);
        given(lockUtil.executeWithLock(
            org.mockito.ArgumentMatchers.eq(lockKey),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS),
            org.mockito.ArgumentMatchers.<Supplier<Object>>any()
        )).willReturn(expectedResult);
        
        // when
        Object result = aspect.executeWithLock(joinPoint, distributedLock);
        
        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(lockUtil).executeWithLock(
            org.mockito.ArgumentMatchers.eq(lockKey),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS),
            org.mockito.ArgumentMatchers.<Supplier<Object>>any()
        );
    }
    
    @Test
    @DisplayName("SpEL 표현식 락 키로 AOP가 동작한다")
    void executeWithSpelLockKey() throws Throwable {
        // given
        String lockKeyExpression = "order:#orderId";
        String expectedLockKey = "order:ORDER-001";
        String expectedResult = "success";
        String orderId = "ORDER-001";
        
        given(distributedLock.key()).willReturn(lockKeyExpression);
        given(distributedLock.waitTime()).willReturn(5L);
        given(distributedLock.leaseTime()).willReturn(15L);
        given(distributedLock.timeUnit()).willReturn(TimeUnit.MINUTES);
        
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(method);
        given(methodSignature.getParameterNames()).willReturn(new String[]{"orderId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{orderId});
        
        given(lockUtil.executeWithLock(
            org.mockito.ArgumentMatchers.eq(expectedLockKey),
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(15L),
            org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES),
            org.mockito.ArgumentMatchers.<Supplier<Object>>any()
        )).willReturn(expectedResult);
        
        // when
        Object result = aspect.executeWithLock(joinPoint, distributedLock);
        
        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(lockUtil).executeWithLock(
            org.mockito.ArgumentMatchers.eq(expectedLockKey),
            org.mockito.ArgumentMatchers.eq(5L),
            org.mockito.ArgumentMatchers.eq(15L),
            org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES),
            org.mockito.ArgumentMatchers.<Supplier<Object>>any()
        );
    }
    
    @Test
    @DisplayName("예외가 발생해도 적절히 처리된다")
    void executeWithException() throws Throwable {
        // given
        String lockKey = "test-lock";
        RuntimeException testException = new RuntimeException("test exception");
        
        given(distributedLock.key()).willReturn(lockKey);
        given(distributedLock.waitTime()).willReturn(10L);
        given(distributedLock.leaseTime()).willReturn(30L);
        given(distributedLock.timeUnit()).willReturn(TimeUnit.SECONDS);
        given(lockUtil.executeWithLock(
            org.mockito.ArgumentMatchers.eq(lockKey),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(30L),
            org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS),
            org.mockito.ArgumentMatchers.<Supplier<Object>>any()
        )).willThrow(testException);
        
        // when & then
        assertThatThrownBy(() -> aspect.executeWithLock(joinPoint, distributedLock))
            .isEqualTo(testException);
    }
}