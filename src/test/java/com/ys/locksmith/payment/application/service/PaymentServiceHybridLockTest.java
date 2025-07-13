package com.ys.locksmith.payment.application.service;

import com.ys.locksmith.common.lock.DistributedLockUtil;
import com.ys.locksmith.payment.application.port.in.PaymentCommand;
import com.ys.locksmith.payment.application.port.out.ExternalPaymentGateway;
import com.ys.locksmith.payment.application.port.out.PaymentGatewayResult;
import com.ys.locksmith.payment.application.port.out.PaymentRepository;
import com.ys.locksmith.payment.domain.Money;
import com.ys.locksmith.payment.domain.Payment;
import com.ys.locksmith.payment.domain.PaymentMethod;
import com.ys.locksmith.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("하이브리드 락 결제 서비스 테스트")
class PaymentServiceHybridLockTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private ExternalPaymentGateway paymentGateway;
    
    @Mock
    private DistributedLockUtil distributedLockUtil;
    
    private PaymentServiceWithHybridLock paymentService;
    
    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceWithHybridLock(paymentRepository, paymentGateway, distributedLockUtil);
    }
    
    @Test
    @DisplayName("하이브리드 락으로 결제를 처리할 수 있다")
    void processPaymentWithHybridLock() {
        // given
        PaymentCommand command = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment savedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        String expectedLockKey = "payment:order:ORDER-001";
        
        given(distributedLockUtil.executeWithLock(eq(expectedLockKey), any(Supplier.class)))
            .willAnswer(invocation -> {
                Supplier<Payment> supplier = invocation.getArgument(1);
                return supplier.get();
            });
        
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment, completedPayment);
        given(paymentGateway.processPayment(anyString(), any(Money.class), any(PaymentMethod.class)))
            .willReturn(PaymentGatewayResult.success("TXN-001"));
        
        // when
        Payment result = paymentService.processPayment(command);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(distributedLockUtil).executeWithLock(eq(expectedLockKey), any(Supplier.class));
        verify(paymentRepository).existsByOrderId("ORDER-001");
        verify(paymentGateway).processPayment("ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
    }
    
    @Test
    @DisplayName("하이브리드 락으로 결제를 취소할 수 있다")
    void cancelPaymentWithHybridLock() {
        // given
        Long paymentId = 1L;
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        Payment cancelledPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        cancelledPayment.complete();
        cancelledPayment.cancel();
        
        String expectedLockKey = "payment:cancel:1";
        
        given(distributedLockUtil.executeWithLock(eq(expectedLockKey), any(Supplier.class)))
            .willAnswer(invocation -> {
                Supplier<Payment> supplier = invocation.getArgument(1);
                return supplier.get();
            });
        
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.of(completedPayment));
        given(paymentGateway.cancelPayment("ORDER-001")).willReturn(true);
        given(paymentRepository.save(any(Payment.class))).willReturn(cancelledPayment);
        
        // when
        Payment result = paymentService.cancelPayment(paymentId);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(distributedLockUtil).executeWithLock(eq(expectedLockKey), any(Supplier.class));
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
        verify(paymentGateway).cancelPayment("ORDER-001");
    }
    
    @Test
    @DisplayName("분산락 내에서 예외 발생 시 적절히 전파된다")
    void exceptionPropagationInDistributedLock() {
        // given
        PaymentCommand command = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        String expectedLockKey = "payment:order:ORDER-001";
        
        RuntimeException testException = new RuntimeException("분산락 내부 예외");
        
        given(distributedLockUtil.executeWithLock(eq(expectedLockKey), any(Supplier.class)))
            .willThrow(testException);
        
        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(command))
            .isEqualTo(testException);
        
        verify(distributedLockUtil).executeWithLock(eq(expectedLockKey), any(Supplier.class));
    }
    
    @Test
    @DisplayName("하이브리드 락에서 비관적 락이 올바르게 사용된다")
    void pessimisticLockUsedInHybridLock() {
        // given
        Long paymentId = 1L;
        String expectedLockKey = "payment:cancel:1";
        
        given(distributedLockUtil.executeWithLock(eq(expectedLockKey), any(Supplier.class)))
            .willAnswer(invocation -> {
                Supplier<Payment> supplier = invocation.getArgument(1);
                return supplier.get();
            });
        
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 정보를 찾을 수 없습니다");
        
        verify(distributedLockUtil).executeWithLock(eq(expectedLockKey), any(Supplier.class));
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
    }
}