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
@DisplayName("결제 서비스 테스트")
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private ExternalPaymentGateway paymentGateway;
    
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentGateway);
    }
    
    @Test
    @DisplayName("결제를 성공적으로 처리할 수 있다")
    void processPaymentSuccess() {
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
    
    @Test
    @DisplayName("외부 게이트웨이 실패 시 결제가 실패 처리된다")
    void processPaymentGatewayFailure() {
        // given
        PaymentCommand command = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment savedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        Payment failedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        failedPayment.fail();
        
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(false);
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment, failedPayment);
        given(paymentGateway.processPayment(anyString(), any(Money.class), any(PaymentMethod.class)))
            .willReturn(PaymentGatewayResult.failure("결제 실패"));
        
        // when
        Payment result = paymentService.processPayment(command);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
    
    @Test
    @DisplayName("이미 존재하는 주문 ID로는 결제할 수 없다")
    void cannotProcessDuplicateOrderId() {
        // given
        PaymentCommand command = PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        given(paymentRepository.existsByOrderId("ORDER-001")).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 주문 ID입니다");
    }
    
    @Test
    @DisplayName("결제 정보를 조회할 수 있다")
    void getPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        
        // when
        Payment result = paymentService.getPayment(1L);
        
        // then
        assertThat(result).isEqualTo(payment);
    }
    
    @Test
    @DisplayName("존재하지 않는 결제 ID로는 조회할 수 없다")
    void cannotGetNonExistentPayment() {
        // given
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> paymentService.getPayment(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 정보를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("완료된 결제를 취소할 수 있다")
    void cancelCompletedPayment() {
        // given
        Payment completedPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        completedPayment.complete();
        Payment cancelledPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        cancelledPayment.complete();
        cancelledPayment.cancel();
        
        given(paymentRepository.findById(1L)).willReturn(Optional.of(completedPayment));
        given(paymentGateway.cancelPayment("ORDER-001")).willReturn(true);
        given(paymentRepository.save(any(Payment.class))).willReturn(cancelledPayment);
        
        // when
        Payment result = paymentService.cancelPayment(1L);
        
        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentGateway).cancelPayment("ORDER-001");
    }
    
    @Test
    @DisplayName("완료되지 않은 결제는 취소할 수 없다")
    void cannotCancelNonCompletedPayment() {
        // given
        Payment pendingPayment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        given(paymentRepository.findById(1L)).willReturn(Optional.of(pendingPayment));
        
        // when & then
        assertThatThrownBy(() -> paymentService.cancelPayment(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("완료된 결제만 취소할 수 있습니다");
    }
    
    @Test
    @DisplayName("유효하지 않은 결제 명령은 검증에 실패한다")
    void validatePaymentCommand() {
        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(
            PaymentCommand.of(null, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
            
        assertThatThrownBy(() -> paymentService.processPayment(
            PaymentCommand.of(1L, null, Money.krw(10000), PaymentMethod.CREDIT_CARD)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("주문 ID는 필수입니다");
            
        assertThatThrownBy(() -> paymentService.processPayment(
            PaymentCommand.of(1L, "ORDER-001", Money.krw(0), PaymentMethod.CREDIT_CARD)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 금액은 양수여야 합니다");
            
        assertThatThrownBy(() -> paymentService.processPayment(
            PaymentCommand.of(1L, "ORDER-001", Money.krw(10000), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("결제 방법은 필수입니다");
    }
}