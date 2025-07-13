package com.ys.locksmith.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("결제 도메인 테스트")
class PaymentTest {
    
    @Test
    @DisplayName("결제를 생성할 수 있다")
    void createPayment() {
        // given
        Long userId = 1L;
        String orderId = "ORDER-001";
        Money amount = Money.krw(10000);
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        
        // when
        Payment payment = Payment.create(userId, orderId, amount, paymentMethod);
        
        // then
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.isPending()).isTrue();
        assertThat(payment.isCompleted()).isFalse();
    }
    
    @Test
    @DisplayName("PENDING 상태의 결제를 완료할 수 있다")
    void completePayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        
        // when
        payment.complete();
        
        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.isPending()).isFalse();
        assertThat(payment.isCompleted()).isTrue();
    }
    
    @Test
    @DisplayName("PENDING이 아닌 상태의 결제는 완료할 수 없다")
    void cannotCompleteNonPendingPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        payment.complete();
        
        // when & then
        assertThatThrownBy(payment::complete)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("결제 상태가 PENDING이 아닙니다");
    }
    
    @Test
    @DisplayName("PENDING 상태의 결제를 실패 처리할 수 있다")
    void failPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        
        // when
        payment.fail();
        
        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.isPending()).isFalse();
        assertThat(payment.isCompleted()).isFalse();
    }
    
    @Test
    @DisplayName("PENDING이 아닌 상태의 결제는 실패 처리할 수 없다")
    void cannotFailNonPendingPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        payment.complete();
        
        // when & then
        assertThatThrownBy(payment::fail)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("결제 상태가 PENDING이 아닙니다");
    }
    
    @Test
    @DisplayName("완료된 결제를 취소할 수 있다")
    void cancelCompletedPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        payment.complete();
        
        // when
        payment.cancel();
        
        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }
    
    @Test
    @DisplayName("PENDING 상태의 결제도 취소할 수 있다")
    void cancelPendingPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        
        // when
        payment.cancel();
        
        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }
    
    @Test
    @DisplayName("실패한 결제는 취소할 수 없다")
    void cannotCancelFailedPayment() {
        // given
        Payment payment = Payment.create(1L, "ORDER-001", Money.krw(10000), PaymentMethod.CREDIT_CARD);
        payment.fail();
        
        // when & then
        assertThatThrownBy(payment::cancel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("실패한 결제는 취소할 수 없습니다");
    }
}