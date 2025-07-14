package com.ys.locksmith.payment.application.service;

import com.ys.locksmith.payment.application.port.out.ExternalPaymentGateway;
import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.Payment;
import com.ys.locksmith.payment.domain.PaymentMethod;
import com.ys.locksmith.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("결제 서비스 캐시 테스트")
class PaymentServiceCacheTest {
    
    @Autowired
    private PaymentServiceWithManualLock paymentService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @MockitoBean
    private PaymentRepository paymentRepository;
    
    @MockitoBean
    private ExternalPaymentGateway paymentGateway;
    
    @Test
    @DisplayName("결제 정보 조회 시 캐시가 적용된다")
    void getPaymentWithCache() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        
        // when - 첫 번째 호출
        Payment result1 = paymentService.getPayment(paymentId);
        
        // when - 두 번째 호출
        Payment result2 = paymentService.getPayment(paymentId);
        
        // then
        assertThat(result1).isEqualTo(result2);
        verify(paymentRepository, times(1)).findById(paymentId);
        
        // 캐시에 저장되었는지 확인
        assertThat(cacheManager.getCache("payment").get(paymentId)).isNotNull();
    }
    
    @Test
    @DisplayName("결제 취소 시 캐시가 무효화된다")
    void cancelPaymentEvictsCache() {
        // given
        Long paymentId = 1L;
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(completedPayment));
        given(paymentGateway.cancelPayment("ORDER-001")).willReturn(true);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));
        
        // 먼저 캐시에 저장
        paymentService.getPayment(paymentId);
        assertThat(cacheManager.getCache("payment").get(paymentId)).isNotNull();
        
        // when - 결제 취소 (캐시 무효화)
        Payment result = paymentService.cancelPayment(paymentId);
        
        // then - 캐시가 무효화되었는지 확인
        assertThat(cacheManager.getCache("payment").get(paymentId)).isNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }
    
    @Test
    @DisplayName("존재하지 않는 결제 ID 조회 시 캐시되지 않는다")
    void getPaymentNotFoundNotCached() {
        // given
        Long paymentId = 999L;
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 정보를 찾을 수 없습니다");
        
        // 캐시에 저장되지 않았는지 확인
        assertThat(cacheManager.getCache("payment").get(paymentId)).isNull();
    }
}