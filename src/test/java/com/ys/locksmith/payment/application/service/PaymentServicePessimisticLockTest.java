package com.ys.locksmith.payment.application.service;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("비관적 락 결제 서비스 테스트")
class PaymentServicePessimisticLockTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private ExternalPaymentGateway paymentGateway;
    
    private PaymentServiceWithPessimisticLock paymentService;
    
    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceWithPessimisticLock(paymentRepository, paymentGateway);
    }
    
    @Test
    @DisplayName("비관적 락을 사용하여 결제 취소를 처리할 수 있다")
    void cancelPaymentWithPessimisticLock() {
        // given
        Long paymentId = 1L;
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        Payment cancelledPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        cancelledPayment.complete();
        cancelledPayment.cancel();
        
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.of(completedPayment));
        given(paymentGateway.cancelPayment("ORDER-001")).willReturn(true);
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
            // 실제 서비스에서 cancel()이 호출된 payment 객체를 그대로 반환
            return invocation.getArgument(0);
        });
        
        // when
        Payment result = paymentService.cancelPayment(paymentId);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
        verify(paymentGateway).cancelPayment("ORDER-001");
    }
    
    @Test
    @DisplayName("존재하지 않는 결제 ID로 비관적 락 조회 시 예외가 발생한다")
    void cancelPaymentNotFoundWithPessimisticLock() {
        // given
        Long paymentId = 999L;
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 정보를 찾을 수 없습니다");
        
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
    }
    
    @Test
    @DisplayName("완료되지 않은 결제는 비관적 락으로도 취소할 수 없다")
    void cannotCancelNonCompletedPaymentWithPessimisticLock() {
        // given
        Long paymentId = 1L;
        Payment pendingPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.of(pendingPayment));
        
        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("완료된 결제만 취소할 수 있습니다");
        
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
    }
    
    @Test
    @DisplayName("결제 게이트웨이 취소 실패 시 예외가 발생한다")
    void cancelPaymentGatewayFailure() {
        // given
        Long paymentId = 1L;
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        given(paymentRepository.findByIdWithPessimisticLock(paymentId)).willReturn(Optional.of(completedPayment));
        given(paymentGateway.cancelPayment("ORDER-001")).willReturn(false);
        
        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("결제 취소 중 오류가 발생했습니다")
            .hasCauseInstanceOf(RuntimeException.class);
        
        verify(paymentRepository).findByIdWithPessimisticLock(paymentId);
        verify(paymentGateway).cancelPayment("ORDER-001");
    }
    
    @Test
    @DisplayName("결제 처리는 기존과 동일하게 동작한다")
    void processPaymentWorksNormally() {
        // given
        PaymentCommand command = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment savedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment, completedPayment);
        given(paymentGateway.processPayment(anyString(), any(Money.class), any(PaymentMethod.class)))
            .willReturn(PaymentGatewayResult.success("TXN-001"));
        
        // when
        Payment result = paymentService.processPayment(command);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).existsByOrderId("ORDER-001");
        verify(paymentGateway).processPayment("ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
    }
}